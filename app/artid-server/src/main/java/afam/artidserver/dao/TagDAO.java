package afam.artidserver.dao;

import afam.artidserver.model.entity.Tag;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagDAO extends ListCrudRepository<Tag, Long> {

    List<Tag> findByIdUser(Long userId);

    boolean existsByTitleAndIdUser(String title, Long userId);

    @Query("""
            SELECT t.* FROM tag t
            JOIN artid_tag at ON at.id_tag = t.id
            JOIN artid a ON a.id = at.id_artid
            WHERE at.id_artid = :artidId
              AND a.id_user = :userId
              AND t.id_user = :userId
              AND a.deleted_at IS NULL
            ORDER BY t.title ASC
            """)
    List<Tag> findByArtidForUser(@Param("artidId") Long artidId, @Param("userId") Long userId);
}
