package afam.artidserver.dao;

import afam.artidserver.model.entity.Resource;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResourceDAO extends ListCrudRepository<Resource, Long> {

  long countByIdUserAndDeletedAtIsNull(Long idUser);

  List<Resource> findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(Long idUser);

  // Materiali collegati a un ArtID. La consistenza di proprietà è imposta dal
  // WHERE: sia l'ArtID
  // sia i materiali devono essere dell'utente loggato (id_user), così il JWT non
  // può leggere
  // materiali altrui nemmeno forzando l'id dell'ArtID. In artid_resource "ar.id"
  // è l'id dell'ArtID.
  @Query("""
      SELECT r.* FROM resource r
      JOIN artid_resource ar ON ar.id_resource = r.id
      JOIN artid a ON a.id = ar.id
      WHERE ar.id = :artidId
        AND a.id_user = :userId
        AND r.id_user = :userId
        AND a.deleted_at IS NULL
        AND r.deleted_at IS NULL
      ORDER BY ar.rank ASC, r.id ASC
      """)
  List<Resource> findByArtidForUser(@Param("artidId") Long artidId, @Param("userId") Long userId);
}
