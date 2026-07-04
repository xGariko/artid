package afam.artidserver.dao;

import afam.artidserver.model.dto.ExternalShareArtIDResponse;
import afam.artidserver.model.entity.ExternalShare;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExternalShareDAO extends ListCrudRepository<ExternalShare, Long> {

    long countByIdCreator(Long idUser);

    @Query("""
            SELECT s.*, a.title, f.file_path
            FROM external_share s
            LEFT JOIN artid a ON s.id_artid = a.id
            LEFT JOIN file f ON f.id = a.id_thumbnail
            WHERE s.id_creator = :userId
            ORDER BY s.created_at
            """)
    List<ExternalShareArtIDResponse> getExternalSharesByUserID(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE external_share s
            SET is_active = false
            WHERE s.id IN (:shareIds) AND s.id_creator = :userId
            """)
    void disableSharesByIds(@Param("userId") Long userId, @Param("shareIds") List<Long> shareIds);

    @Modifying
    @Query("""
            UPDATE external_share s
            SET is_active = true
            WHERE s.id IN (:shareIds) AND s.id_creator = :userId
            """)
    void enableSharesByIds(@Param("userId") Long userId, @Param("shareIds") List<Long> shareIds);

    @Modifying
    @Query(value = """
        DELETE FROM external_share s
        WHERE s.id IN (:shareIds) AND s.id_creator = :userId
        """)
    void deleteSharesByIds(@Param("userId") Long userId, @Param("shareIds") List<Long> shareIds);
}
