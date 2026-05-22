package afam.artidserver.dao;

import afam.artidserver.model.entity.ExternalShare;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface ExternalShareDAO extends ListCrudRepository<ExternalShare, Long> {

    @Query("""
            SELECT COUNT(*)
            FROM external_share es
            JOIN artid a ON es.id_artid = a.id
            WHERE a.id_user = :userId AND a.deleted_at IS NULL
            """)
    long countByArtidOwner(@Param("userId") Long userId);
}
