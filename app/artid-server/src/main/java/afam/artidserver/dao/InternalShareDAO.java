package afam.artidserver.dao;

import afam.artidserver.model.entity.InternalShare;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

//@Repository
//@RequiredArgsConstructor
//public class InternalShareDAO { // Non so perchè prima era fatta così(?) ho commentato il codice originale
public interface InternalShareDAO extends ListCrudRepository<InternalShare, Long> {

//    private final NamedParameterJdbcTemplate jdbc;

//    public long countByArtidOwner(Long userId) {
//        String sql = """
//                SELECT COUNT(*)
//                FROM internal_share s
//                JOIN artid a ON s.id_artid = a.id
//                WHERE a.id_user = :userId AND a.deleted_at IS NULL
//                """;
//        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Long.class);
//        return count != null ? count : 0L;
//    }

    long countByIdUserFrom(Long idUser);

    @Query("""
            SELECT *
            FROM internal_share s
            WHERE s.id_user_from = :userId
            """)
    List<InternalShare> getInternalSharesByUserID(@Param("userId") Long userId);
}
