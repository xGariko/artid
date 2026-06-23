package afam.artidserver.dao;

import afam.artidserver.model.dto.ExternalShareArtIDResponse;
import afam.artidserver.model.entity.ExternalShare;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExternalShareDAO extends ListCrudRepository<ExternalShare, Long> {

    @Query("""
            SELECT COUNT(*)
            FROM external_share es
            JOIN artid a ON es.id_artid = a.id
            WHERE a.id_user = :userId AND a.deleted_at IS NULL
            """)
    long countByArtidOwner(@Param("userId") Long userId);

    @Query("""
            SELECT es.*, a.title, a.id_thumbnail
            FROM external_share es
            JOIN artid a ON es.id_artid = a.id
            WHERE a.id_user = :userId AND a.deleted_at IS NULL
            """)
    List<ExternalShareArtIDResponse> getExternalSharesByUserID(@Param("userId") Long userId);

//    List<ExternalShare> findByUserId(Long userId); //non si può fare per ora perchè non ha userID
}
