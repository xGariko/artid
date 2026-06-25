package afam.artidserver.service;

import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.dto.PublicArtidSummaryResponse;
import afam.artidserver.model.dto.PublicCertificationResponse;
import afam.artidserver.model.dto.PublicProfileDetailResponse;
import afam.artidserver.model.dto.PublicProfileResponse;
import afam.artidserver.model.entity.User;
import afam.artidserver.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDAO userRepository;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final AvatarService avatarService;
    private final StorageService storageService;

    @Value("${supabase.s3.propics-bucket}")
    private String propicsBucket;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // Ricerca "intelligente" del catalogo pubblico. Un utente è rilevante se è pubblico E
    // (matcha per nome/cognome/professione) OPPURE possiede almeno un ArtID PUBBLICO il cui
    // titolo matcha. Il conteggio mostrato è il totale dei suoi ArtID pubblici (non solo i match).
    // Ordinamento per rilevanza: prima i match su persona, poi su professione, infine solo-ArtID.
    // "user" è parola riservata → quotata.
    private static final String SEARCH_PUBLIC_PROFILES_SQL = """
            SELECT u.id, u.name, u.surname, u.profession,
                   (SELECT COUNT(*) FROM artid a
                     WHERE a.id_user = u.id AND a.visibility_state = 'public' AND a.deleted_at IS NULL
                   ) AS public_artid_count
            FROM "user" u
            WHERE u.is_public = TRUE
              AND u.deleted_at IS NULL
              AND (
                    :blankQuery = TRUE
                 OR u.name ILIKE :pattern
                 OR u.surname ILIKE :pattern
                 OR (COALESCE(u.name, '') || ' ' || COALESCE(u.surname, '')) ILIKE :pattern
                 OR u.profession ILIKE :pattern
                 OR EXISTS (
                        SELECT 1 FROM artid a
                         WHERE a.id_user = u.id AND a.visibility_state = 'public' AND a.deleted_at IS NULL
                           AND a.title ILIKE :pattern
                    )
              )
            ORDER BY
                CASE
                    WHEN u.name ILIKE :pattern
                      OR u.surname ILIKE :pattern
                      OR (COALESCE(u.name, '') || ' ' || COALESCE(u.surname, '')) ILIKE :pattern THEN 0
                    WHEN u.profession ILIKE :pattern THEN 1
                    ELSE 2
                END,
                u.surname, u.name
            """;

    private static final RowMapper<PublicProfileResponse> PUBLIC_PROFILE_ROW_MAPPER = (rs, rowNum) ->
            new PublicProfileResponse(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("surname"),
                    rs.getString("profession"),
                    rs.getLong("public_artid_count")
            );

    // Attestati pubblici dell'utente (solo is_public = TRUE).
    private static final String PUBLIC_CERTIFICATIONS_SQL = """
            SELECT id, title
              FROM certifications
             WHERE id_user = :userId AND is_public = TRUE
             ORDER BY id
            """;

    // ArtID pubblici dell'utente col conteggio delle risorse collegate (non eliminate). Nella
    // join artid_resource la colonna "id" è l'id dell'ArtID, "id_resource" quella della risorsa.
    private static final String PUBLIC_ARTIDS_SQL = """
            SELECT a.id, a.title, a.created_at,
                   (SELECT COUNT(*) FROM artid_resource ar
                      JOIN resource r ON r.id = ar.id_resource
                     WHERE ar.id = a.id AND r.deleted_at IS NULL) AS resource_count
              FROM artid a
             WHERE a.id_user = :userId AND a.visibility_state = 'public' AND a.deleted_at IS NULL
             ORDER BY a.created_at DESC
            """;

    // Object key (bucket risorse di default) dei file collegati a risorse, certificazioni e
    // thumbnail degli ArtID dell'utente: servono per cancellarli da Storage durante l'erasure GDPR.
    private static final String USER_FILE_KEYS_SQL = """
            SELECT f.id, f.file_path
              FROM file f
             WHERE f.id IN (
                   SELECT id_file      FROM resource       WHERE id_user = :userId AND id_file IS NOT NULL
                   UNION
                   SELECT id_file      FROM certifications  WHERE id_user = :userId AND id_file IS NOT NULL
                   UNION
                   SELECT id_thumbnail FROM artid           WHERE id_user = :userId AND id_thumbnail IS NOT NULL
             )
            """;

    // audit_log resta (FK in SET NULL) ma ip_address/user_agent/old_value/new_value sono dati
    // personali: vanno azzerati prima del delete, finché id_user identifica ancora le sue righe.
    private static final String ANONYMIZE_AUDIT_LOG_SQL = """
            UPDATE audit_log
               SET ip_address = NULL, user_agent = NULL, old_value = NULL, new_value = NULL
             WHERE id_user = :userId
            """;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByMail(String mail) {
        return userRepository.findByMail(mail);
    }

    // Lookup per identità digitale: l'unico utente che ha collegato quel codice SPID (unicità
    // garantita dall'indice parziale su spid_code, vedi migration V11). Usato per impedire che la
    // stessa identità venga associata a più account.
    public Optional<User> findBySpidCode(String spidCode) {
        return userRepository.findBySpidCode(spidCode);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Cancellazione FISICA dell'utente (GDPR art. 17, diritto all'oblio). Il DELETE su "user"
     * propaga via ON DELETE CASCADE a tutte le righe collegate (artid, resource, certifications,
     * internal/external_share, artid_resource, login_otp; vedi migration V9). Restano da rimuovere
     * i dati personali NON coperti dal cascade: le righe `file` (PADRE di resource/certifications,
     * non figlie) e gli oggetti su Supabase Storage (foto profilo + file). Le object key vanno
     * raccolte PRIMA del delete; gli oggetti si rimuovono dopo il commit (best-effort).
     */
    @Transactional
    public void deleteById(Long id) {
        String propicKey = userRepository.findById(id).map(User::getPropicPath).orElse(null);

        List<Long> fileIds = new ArrayList<>();
        List<String> fileKeys = new ArrayList<>();
        namedJdbcTemplate.query(USER_FILE_KEYS_SQL, new MapSqlParameterSource("userId", id), rs -> {
            fileIds.add(rs.getLong("id"));
            String key = rs.getString("file_path");
            if (key != null && !key.isBlank()) fileKeys.add(key);
        });

        // audit_log: anonimizza le sue righe PRIMA del delete (poi la FK le staccherà con SET NULL).
        namedJdbcTemplate.update(ANONYMIZE_AUDIT_LOG_SQL, new MapSqlParameterSource("userId", id));

        // Cascade DB: rimuove l'utente e tutte le righe che lo referenziano.
        userRepository.deleteById(id);

        // Le righe `file` non sono coperte dal cascade (sono il padre di resource/certifications):
        // ora che le figlie sono sparite, si possono eliminare.
        if (!fileIds.isEmpty()) {
            namedJdbcTemplate.update("DELETE FROM file WHERE id IN (:ids)",
                    new MapSqlParameterSource("ids", fileIds));
        }

        // Foto profilo (bucket propics) + file risorse/certificazioni (bucket default): rimossi
        // dopo il commit, così un eventuale rollback non lascia il DB con byte già cancellati.
        deleteFromStorageAfterCommit(propicKey, fileKeys);
    }

    private void deleteFromStorageAfterCommit(String propicKey, List<String> fileKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            purgeStorage(propicKey, fileKeys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                purgeStorage(propicKey, fileKeys);
            }
        });
    }

    private void purgeStorage(String propicKey, List<String> fileKeys) {
        if (propicKey != null && !propicKey.isBlank()) {
            safeDeleteFromBucket(propicsBucket, propicKey);
        }
        for (String key : fileKeys) {
            safeDeleteFromBucket(null, key); // null = bucket di default (risorse)
        }
    }

    // Pulizia Storage best-effort: un fallimento non deve far fallire l'erasure (il DB, fonte di
    // verità, è già committato). L'oggetto resterebbe orfano su S3, da ripulire a parte.
    private void safeDeleteFromBucket(String bucket, String objectKey) {
        try {
            if (bucket != null) storageService.delete(bucket, objectKey);
            else storageService.delete(objectKey);
        } catch (RuntimeException e) {
            logger.warn("Impossibile eliminare l'oggetto Storage '{}': {}", objectKey, e.getMessage());
        }
    }

    /**
     * Cerca i profili PUBBLICI rilevanti per la query (vedi {@link #SEARCH_PUBLIC_PROFILES_SQL}).
     * Con query vuota restituisce l'intero catalogo pubblico (browse).
     */
    public List<PublicProfileResponse> searchPublicProfiles(String query) {
        String trimmed = query == null ? "" : query.trim();
        boolean blankQuery = trimmed.isEmpty();
        // Su query vuota '%' matcha tutti (catalogo completo). I metacaratteri LIKE digitati
        // dall'utente vengono neutralizzati con escape backslash (default Postgres).
        String pattern = blankQuery ? "%" : "%" + escapeLike(trimmed) + "%";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("pattern", pattern)
                .addValue("blankQuery", blankQuery);

        return namedJdbcTemplate.query(SEARCH_PUBLIC_PROFILES_SQL, params, PUBLIC_PROFILE_ROW_MAPPER);
    }

    /**
     * Dettaglio di un profilo pubblico per la pagina Explore. Restituisce dati SOLO se l'utente
     * è pubblico e non eliminato; un utente inesistente, privato o soft-deleted dà Optional.empty()
     * (il controller risponde 404 indistintamente, senza rivelare l'esistenza di profili privati).
     * Espone esclusivamente campi non sensibili e contenuti pubblici (certificazioni e ArtID).
     */
    public Optional<PublicProfileDetailResponse> getPublicProfileDetail(Long id) {
        return userRepository.findById(id)
                .filter(u -> Boolean.TRUE.equals(u.getIsPublic()) && u.getDeletedAt() == null)
                .map(this::toPublicDetail);
    }

    private PublicProfileDetailResponse toPublicDetail(User user) {
        Long userId = user.getId();
        boolean verified = user.getSpidCode() != null && !user.getSpidCode().isBlank();
        return new PublicProfileDetailResponse(
                userId,
                user.getName(),
                user.getSurname(),
                user.getProfession(),
                user.getBirthplace(),
                avatarService.presignKey(user.getPropicPath()),
                verified,
                user.getLinkedinId(),
                user.getBusinessEmail(),
                findPublicCertifications(userId),
                findPublicArtids(userId)
        );
    }

    private List<PublicCertificationResponse> findPublicCertifications(Long userId) {
        return namedJdbcTemplate.query(
                PUBLIC_CERTIFICATIONS_SQL,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new PublicCertificationResponse(rs.getLong("id"), rs.getString("title"))
        );
    }

    private List<PublicArtidSummaryResponse> findPublicArtids(Long userId) {
        return namedJdbcTemplate.query(
                PUBLIC_ARTIDS_SQL,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new PublicArtidSummaryResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getLong("resource_count")
                )
        );
    }

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
