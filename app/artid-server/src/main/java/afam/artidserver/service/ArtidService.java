package afam.artidserver.service;

import afam.artidserver.dao.ArtidDAO;
import afam.artidserver.dao.FileDAO;
import afam.artidserver.model.VISIBILITY_STATE;
import afam.artidserver.model.dto.ArtidDetailsUpdateRequest;
import afam.artidserver.model.dto.ArtidResponse;
import afam.artidserver.model.entity.Artid;
import afam.artidserver.model.entity.File;
import afam.artidserver.storage.StorageService;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArtidService {

    private static final Logger logger = LoggerFactory.getLogger(ArtidService.class);

    // Limite dimensione thumbnail, coerente con l'avatar (vedi AvatarService).
    private static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024; // 5MB

    private final ArtidDAO artidDAO;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final StorageService storageService;
    private final FileDAO fileDAO;

    @Value("${supabase.s3.presign-ttl-seconds}")
    private long presignTtlSeconds;

    public long countByUser(Long userId) {
        return artidDAO.countByIdUserAndDeletedAtIsNull(userId);
    }

    public List<ArtidResponse> findByUser(Long userId) {
        List<Artid> artids = artidDAO.findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(userId);
        Map<Long, String> thumbnailUrls = presignThumbnails(artids);
        return artids.stream()
                .map(a -> {
                    Long thumbnailId = a.getIdThumbnail();
                    return toResponse(a, thumbnailId != null ? thumbnailUrls.get(thumbnailId) : null);
                })
                .toList();
    }

    @Transactional
    public boolean delete(Long id, Long userId) {
        return artidDAO.findById(id).filter(artid -> userId.equals(artid.getIdUser())).map(artid -> {
            jdbcTemplate.update("DELETE FROM artid_resource WHERE id = ?", id);
            // TODO delete condivisioni
            artidDAO.deleteById(id);
            return true;
        }).orElse(false);
    }

    /**
     * Dettaglio di un singolo ArtID dell'utente loggato. L'ownership è imposta
     * dalla query
     * (filtra per id_user): un ArtID non di proprietà, inesistente o eliminato dà
     * Optional.empty()
     * → il controller risponde 404 senza rivelare se esiste. Impossibile leggere
     * l'ArtID di un
     * altro utente forzando l'id nell'URL.
     */
    public Optional<ArtidResponse> findByIdForUser(Long id, Long userId) {
        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(id, userId)
                .map(a -> toResponse(a, presignThumbnail(a.getIdThumbnail())));
    }

    /**
     * Crea un nuovo ArtID intestato all'utente loggato (solo titolo). I booleani e
     * i timestamp
     * sono valorizzati esplicitamente: con Spring Data JDBC l'INSERT scrive tutte
     * le colonne,
     * quindi non ci si affida ai DEFAULT del DB (altrimenti i NOT NULL andrebbero a
     * NULL).
     */
    @Transactional
    public ArtidResponse create(String title, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        // Default applicativo: nuovo ArtID privato. Validato comunque contro le
        // etichette ammesse,
        // così se un giorno la visibilità arrivasse dal client un valore fuori enum
        // darebbe 400.
        String visibility = requireValidVisibility(VISIBILITY_STATE.PRIVATE.getLabel());

        // INSERT manuale (non save()): visibility_state è un enum Postgres e un
        // parametro String va
        // castato esplicitamente, altrimenti il driver invia un varchar che Postgres
        // rifiuta.
        Long id = namedJdbcTemplate.queryForObject("""
                INSERT INTO artid (id_user, title, favourite, created_at, last_modified, visibility_state)
                VALUES (:idUser, :title, false, :now, :now, CAST(:visibility AS visibility_state))
                RETURNING id
                """,
                new MapSqlParameterSource()
                        .addValue("idUser", userId)
                        .addValue("title", title.trim())
                        .addValue("now", now)
                        .addValue("visibility", visibility),
                Long.class);

        Artid artid = new Artid();
        artid.setId(id);
        artid.setIdUser(userId);
        artid.setTitle(title.trim());
        artid.setFavourite(false);
        artid.setCreatedAt(now);
        artid.setLastModified(now);
        artid.setVisibilityState(visibility);
        return toResponse(artid, null);
    }

    // Le etichette valide sono quelle dell'enum Postgres visibility_state. Un
    // valore non ammesso
    // diventa 400 invece di propagarsi al DB e far fallire il CAST con un 500.
    private static String requireValidVisibility(String value) {
        try {
            return VISIBILITY_STATE.fromLabel(value).getLabel();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stato di visibilità non valido: " + value);
        }
    }

    @Transactional
    public boolean linkArtidResource(Long id, Long resourceId, Long userId) {
        // Conta quante risorse ha già questo specifico ArtID
        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(id, userId)
                .map(artid -> {

                    // Controllo duplicati: il materiale è già associato a questo ArtID?
                    if (artidDAO.existsResourceInArtid(id, resourceId)) {
                        return false; // Evita l'inserimento duplicato
                    }
                    // Se esiste ed è mio, conto i materiali già presenti
                    long currentCount = artidDAO.countResourcesByArtidId(id);

                    // Calcolo il rank successivo
                    int nextRank = (int) currentCount + 1;

                    // Eseguo la insert
                    jdbcTemplate.update(
                            "INSERT INTO artid_resource (id_resource, id, rank) VALUES (?, ?, ?)",
                            resourceId, id, nextRank);

                    return true; // Operazione riuscita
                })
                .orElse(false); // Artid non trovato o non di proprietà dell'utente
    }

    @Transactional
    public void linkArtidTag(Long id, Long tagId) {
        jdbcTemplate.update(
                "INSERT INTO artid_tag (id_tag, id_artid) VALUES (?, ?)",
                tagId, id);
    }

    private static ArtidResponse toResponse(Artid a, String thumbnailUrl) {
        return new ArtidResponse(
                a.getId(),
                a.getIdUser(),
                a.getIdThumbnail(),
                a.getTitle(),
                a.getDescription(),
                a.getFavourite(),
                a.getVisibilityState(),
                a.getCreatedAt(),
                a.getLastModified(),
                thumbnailUrl);
    }

    // Presigned GET URL della thumbnail (bucket di default, come i materiali); null
    // se assente.
    private String presignThumbnail(Long idThumbnail) {
        if (idThumbnail == null)
            return null;
        return fileDAO.findById(idThumbnail)
                .map(File::getFilePath)
                .filter(key -> key != null && !key.isBlank())
                .map(key -> storageService.presignGet(key, Duration.ofSeconds(presignTtlSeconds)))
                .orElse(null);
    }

    // Presigna in un colpo solo le thumbnail di più ArtID: una sola query su file,
    // niente N+1.
    private Map<Long, String> presignThumbnails(List<Artid> artids) {
        List<Long> thumbnailIds = artids.stream()
                .map(Artid::getIdThumbnail)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (thumbnailIds.isEmpty())
            return Map.of();

        Map<Long, String> urlByThumbnailId = new HashMap<>();
        namedJdbcTemplate.query(
                "SELECT id, file_path FROM file WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", thumbnailIds),
                rs -> {
                    String key = rs.getString("file_path");
                    if (key != null && !key.isBlank()) {
                        urlByThumbnailId.put(rs.getLong("id"),
                                storageService.presignGet(key, Duration.ofSeconds(presignTtlSeconds)));
                    }
                });
        return urlByThumbnailId;
    }

    /**
     * Rimuove l'associazione tra un Artid e un Materiale.
     * 
     * @return true se la cancellazione è avvenuta, false se l'associazione non
     *         esisteva.
     */
    @Transactional
    public boolean removeResourceFromArtid(Long artidId, Long resourceId, Long userId) {
        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(artidId, userId)
                .map(artid -> {

                    Optional<Integer> rankOpt = artidDAO.findRankByArtidIdAndResourceId(artidId, resourceId);

                    if (rankOpt.isEmpty()) {
                        return false; // Il legame risorsa-artid non esiste
                    }
                    int deletedRank = rankOpt.get();

                    // 2. Cancella la risorsa
                    int rowsAffected = artidDAO.removeResourceByResourceId(artidId, resourceId);

                    // 3. Se è stata cancellata con successo, aggiorna i rank rimasti
                    if (rowsAffected > 0) {
                        artidDAO.decrementRanksAfterDeletion(artidId, deletedRank);
                        return true;
                    }

                    return false;
                })
                .orElse(false);

    }

    @Transactional
    public boolean reorderResource(Long artidId, Long resourceId, Integer newRank, Long userId) {
        // 1. Controllo di sicurezza: l'artid esiste ed è dell'utente autenticato?
        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(artidId, userId)
                .map(artid -> {

                    // 2. Recuperiamo il vecchio rank del materiale prima dello spostamento
                    Optional<Integer> oldRankOpt = artidDAO.findRankByArtidIdAndResourceId(artidId, resourceId);
                    if (oldRankOpt.isEmpty()) {
                        return false; // Il legame risorsa-artid non esiste
                    }
                    int oldRank = oldRankOpt.get();

                    // Se la posizione è la stessa, non c'è bisogno di fare query
                    if (oldRank == newRank)
                        return true;

                    // 3. Eseguiamo lo shift corretto in base alla direzione
                    if (oldRank < newRank) {
                        // Il materiale è sceso (es: da posizione 2 a 5)
                        artidDAO.shiftRanksUp(artidId, oldRank, newRank);
                    } else {
                        // Il materiale è salito (es: da posizione 5 a 2)
                        artidDAO.shiftRanksDown(artidId, oldRank, newRank);
                    }

                    // 4. Aggiorniamo il rank del materiale spostato col suo nuovo valore definitivo
                    artidDAO.updateResourceRank(artidId, resourceId, newRank);

                    return true;
                })
                .orElse(false); // 404 se l'ArtID non è tuo o non esiste
    }

    /**
     * Aggiorna lo stato di "preferito" di un ArtID.
     * L'ownership è forzata: se l'ArtID non appartiene all'utente,
     * il metodo restituisce Optional.empty() (che si tradurrà in un 404).
     */
    @Transactional
    public boolean updateFavourite(Long id, boolean favourite, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Cerchiamo l'ArtID verificando che appartenga all'utente e non sia
        // eliminato
        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(id, userId)
                .map(artid -> {

                    jdbcTemplate.update(
                            """
                                    UPDATE artid
                                    SET favourite = ?, last_modified = ?
                                    WHERE id = ?
                                    """,
                            favourite, now, id);

                    return true;
                }).orElse(false);
    }

    @Transactional
    public boolean updateDetails(Long id, Long userId, ArtidDetailsUpdateRequest request, MultipartFile image) {
        OffsetDateTime now = OffsetDateTime.now();

        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(id, userId)
                .map(artid -> {
                    // Se il titolo è stato passato, lo aggiorniamo
                    if (request.title() != null) {
                        artid.setTitle(request.title().trim());
                    }

                    // Se la descrizione è stata passata, la aggiorniamo
                    if (request.description() != null) {
                        artid.setDescription(request.description().trim());
                    }

                    // Nuova thumbnail: la carichiamo su Storage e registriamo un record File (come
                    // i
                    // materiali, bucket di default). Il vecchio file va cancellato solo DOPO
                    // l'UPDATE che
                    // sposta id_thumbnail sul nuovo record, altrimenti la FK punta ancora al
                    // vecchio.
                    Long oldThumbnailId = null;
                    String oldThumbnailKey = null;
                    if (image != null && !image.isEmpty()) {
                        oldThumbnailId = artid.getIdThumbnail();
                        oldThumbnailKey = oldThumbnailId != null
                                ? fileDAO.findById(oldThumbnailId).map(File::getFilePath).orElse(null)
                                : null;
                        File newThumbnail = saveThumbnailFile(image);
                        artid.setIdThumbnail(newThumbnail.getId());
                    }

                    artid.setLastModified(now);

                    jdbcTemplate.update(
                            """
                                    UPDATE artid
                                    SET title = ?, description = ?, id_thumbnail = ?, last_modified = ?
                                    WHERE id = ?
                                    """,
                            artid.getTitle(), artid.getDescription(), artid.getIdThumbnail(), now, id);

                    if (oldThumbnailId != null) {
                        fileDAO.deleteById(oldThumbnailId);
                        deleteObjectAfterCommit(oldThumbnailKey);
                    }

                    return true;
                })
                .orElse(false);
    }

    /**
     * Carica i byte della thumbnail su Storage (bucket di default, come i
     * materiali) e registra il
     * record {@link File} con la object key. La thumbnail deve essere un'immagine.
     * Se la transazione
     * fa rollback l'oggetto appena caricato viene rimosso, per non lasciare orfani
     * su Storage.
     */
    private File saveThumbnailFile(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La thumbnail deve essere un'immagine");
        }
        if (image.getSize() > MAX_THUMBNAIL_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Immagine troppo grande (max 5MB)");
        }

        String objectKey = storageService.newObjectKey(image.getOriginalFilename());
        try {
            storageService.upload(objectKey, image.getInputStream(), image.getSize(), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Errore lettura della thumbnail in upload", e);
        }
        deleteObjectOnRollback(objectKey);

        File file = new File();
        file.setFilePath(objectKey);
        file.setFileName(image.getOriginalFilename());
        file.setMimeType(contentType);
        file.setExtension(extractExtension(image.getOriginalFilename()));
        file.setFileSize(image.getSize());
        return fileDAO.save(file);
    }

    // Cancella l'oggetto su Storage solo dopo il commit (vecchia thumbnail
    // sostituita).
    private void deleteObjectAfterCommit(String objectKey) {
        if (objectKey == null)
            return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDelete(objectKey);
            }
        });
    }

    // Rimuove l'oggetto appena caricato se la transazione fa rollback (evita orfani
    // su Storage).
    private void deleteObjectOnRollback(String objectKey) {
        if (objectKey == null || !TransactionSynchronizationManager.isSynchronizationActive())
            return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    safeDelete(objectKey);
                }
            }
        });
    }

    // La pulizia su Storage è best-effort: un fallimento non deve propagarsi (il DB
    // è la fonte di verità).
    private void safeDelete(String objectKey) {
        try {
            storageService.delete(objectKey);
        } catch (RuntimeException e) {
            logger.warn("Impossibile eliminare l'oggetto Storage '{}': {}", objectKey, e.getMessage());
        }
    }

    private static String extractExtension(String fileName) {
        if (fileName == null)
            return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }

    @Transactional
    public boolean updateVisibility(Long id, String visibility, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();

        String validVisibility = requireValidVisibility(visibility);

        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(id, userId)
                .map(artid -> {
                    jdbcTemplate.update(
                            """
                                    UPDATE artid
                                    SET visibility_state = ?::visibility_state, last_modified = ?
                                    WHERE id = ?
                                    """,
                            validVisibility,
                            now,
                            id);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean removeTagFromArtid(Long artidId, Long tagId, Long userId) {

        return artidDAO.findByIdAndIdUserAndDeletedAtIsNull(artidId, userId)
                .map(artid -> {
                    int rowsAffected = artidDAO.removeTagByTagId(artidId, tagId);
                    return rowsAffected > 0;
                })
                .orElse(false);
    }
}
