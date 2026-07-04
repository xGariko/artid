package afam.artidserver.dao;

import afam.artidserver.model.entity.Artid;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtidDAO extends ListCrudRepository<Artid, Long> {

    long countByIdUserAndDeletedAtIsNull(Long idUser);

    List<Artid> findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(Long idUser);

    // Ownership a livello di query: l'id_user è nel WHERE, quindi un ArtID di un
    // altro
    // utente (o inesistente / soft-deleted) non viene MAI restituito → 404
    // indistinguibile.
    Optional<Artid> findByIdAndIdUserAndDeletedAtIsNull(Long id, Long idUser);

    @Modifying
    @Query("""
            DELETE FROM artid_resource
            WHERE id = :artidId AND id_resource = :resourceId
            """)
    int removeResourceByResourceId(@Param("artidId") Long id, @Param("resourceId") Long resourceId);

    @Modifying
    @Query("""
            DELETE FROM artid_tag
            WHERE id_artid = :artidId AND id_tag = :tagId
            """)
    int removeTagByTagId(@Param("artidId") Long id, @Param("tagId") Long tagId);

}
