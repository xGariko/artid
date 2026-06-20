package afam.artidserver.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InternalShareDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public long countByArtidOwner(Long userId) {
        String sql = """
                SELECT COUNT(*)
                FROM internal_share s
                JOIN artid a ON s.id_artid = a.id
                WHERE a.id_user = :userId AND a.deleted_at IS NULL
                """;
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Long.class);
        return count != null ? count : 0L;
    }
}
