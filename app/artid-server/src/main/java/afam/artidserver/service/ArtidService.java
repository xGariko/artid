package afam.artidserver.service;

import afam.artidserver.dao.ArtidDAO;
import afam.artidserver.model.VISIBILITY_STATE;
import afam.artidserver.model.dto.ArtidDetailsUpdateRequest;
import afam.artidserver.model.dto.ArtidResponse;
import afam.artidserver.model.entity.Artid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArtidService {

    private final ArtidDAO artidDAO;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public long countByUser(Long userId) {
        return artidDAO.countByIdUserAndDeletedAtIsNull(userId);
    }

    public List<ArtidResponse> findByUser(Long userId) {
        return artidDAO.findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(userId)
                .stream()
                .map(ArtidService::toResponse)
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
                .map(ArtidService::toResponse);
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
        return toResponse(artid);
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
    public void linkArtidResource(Long id, Long resourceId) {
        jdbcTemplate.update(
                "INSERT INTO artid_resource (id_resource, id, rank) VALUES (?, ?, 0)",
                resourceId, id);
    }

    private static ArtidResponse toResponse(Artid a) {
        return new ArtidResponse(
                a.getId(),
                a.getIdUser(),
                a.getIdThumbnail(),
                a.getTitle(),
                a.getDescription(),
                a.getFavourite(),
                a.getVisibilityState(),
                a.getCreatedAt(),
                a.getLastModified());
    }

    /**
     * Rimuove l'associazione tra un Artid e un Materiale.
     * 
     * @return true se la cancellazione è avvenuta, false se l'associazione non
     *         esisteva.
     */
    @Transactional
    public boolean removeResourceFromArtid(Long artidId, Long resourceId) {
        // Esegue la DELETE e controlla se il numero di righe eliminate è maggiore di 0
        int rowsAffected = artidDAO.removeResourceByResourceId(artidId, resourceId);
        return rowsAffected > 0;
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

                    // Se c'è un file immagine valido, lo gestisci qui
                    if (image != null && !image.isEmpty()) {
                        // Logica di salvataggio del file dell'immagine...
                    }

                    artid.setLastModified(now);

                    jdbcTemplate.update(
                            """
                                    UPDATE artid
                                    SET title = ?, description = ?, last_modified = ?
                                    WHERE id = ?
                                    """,
                            artid.getTitle(), artid.getDescription(), now, id);

                    return true;
                })
                .orElse(false);
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
}
