package afam.artidserver.dao;

import afam.artidserver.model.entity.Artid;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface ArtidDAO extends ListCrudRepository<Artid, Long> {

    long countByIdUserAndDeletedAtIsNull(Long idUser);

    List<Artid> findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(Long idUser);

    // Ownership a livello di query: l'id_user è nel WHERE, quindi un ArtID di un altro
    // utente (o inesistente / soft-deleted) non viene MAI restituito → 404 indistinguibile.
    Optional<Artid> findByIdAndIdUserAndDeletedAtIsNull(Long id, Long idUser);
}
