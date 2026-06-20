package afam.artidserver.service;

import afam.artidserver.dao.ArtidDAO;
import afam.artidserver.model.dto.ArtidResponse;
import afam.artidserver.model.entity.Artid;
import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Artid artid = new Artid();
        artid.setIdUser(userId);
        artid.setTitle(title.trim());
        artid.setFavourite(false);
        artid.setIsPublic(false);
        artid.setIsPrivate(false);
        OffsetDateTime now = OffsetDateTime.now();
        artid.setCreatedAt(now);
        artid.setLastModified(now);
        return toResponse(artidDAO.save(artid));
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
                a.getTitle(),
                a.getDescription(),
                a.getFavourite(),
                a.getCreatedAt(),
                a.getLastModified(),
                a.getIsPublic(),
                a.getIsPrivate());
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
}
