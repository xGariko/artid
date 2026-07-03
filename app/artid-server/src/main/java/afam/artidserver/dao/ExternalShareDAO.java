package afam.artidserver.dao;

import afam.artidserver.model.dto.ExternalShareArtIDResponse;
import afam.artidserver.model.entity.ExternalShare;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExternalShareDAO extends ListCrudRepository<ExternalShare, Long> {

    long countByIdUser(Long idUser);

    @Query("""
            SELECT s.*, a.title, f.file_path
            FROM external_share s
            LEFT JOIN artid a ON s.id_artid = a.id
            LEFT JOIN file f ON f.id = a.id_thumbnail
            WHERE s.id_user = :userId
            """)
    List<ExternalShareArtIDResponse> getExternalSharesByUserID(@Param("userId") Long userId);

}
