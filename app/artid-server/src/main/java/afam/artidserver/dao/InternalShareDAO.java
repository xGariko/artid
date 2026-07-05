package afam.artidserver.dao;

import afam.artidserver.model.dto.InternalShareArtIDExtendedResponse;
import afam.artidserver.model.dto.InternalShareArtIDResponse;
import afam.artidserver.model.entity.InternalShare;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

//@Repository
//@RequiredArgsConstructor
//public class InternalShareDAO { // Non so perchè prima era fatta così(?) ho commentato il codice originale
public interface InternalShareDAO extends ListCrudRepository<InternalShare, Long> {

    // private final NamedParameterJdbcTemplate jdbc;

    // public long countByArtidOwner(Long userId) {
    // String sql = """
    // SELECT COUNT(*)
    // FROM internal_share s
    // JOIN artid a ON s.id_artid = a.id
    // WHERE a.id_user = :userId AND a.deleted_at IS NULL
    // """;
    // Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId",
    // userId), Long.class);
    // return count != null ? count : 0L;
    // }

    long countByIdUserFrom(Long idUser);

    long countByIdUserTo(Long idUser);

    @Query("""
            SELECT s.*, a.title, f.file_path
            FROM internal_share s
            JOIN artid a ON s.id_artid = a.id
            LEFT JOIN file f ON a.id_thumbnail = f.id
            WHERE s.id_user_from = :userId
            ORDER BY s.created_at
            """)
    List<InternalShareArtIDResponse> getInternalSharesFromUserID(@Param("userId") Long userId);

    @Query("""
            SELECT s.*, a.title, f.file_path, u.name
            FROM internal_share s
            JOIN artid a ON s.id_artid = a.id
            JOIN "user" u ON u.id = s.id_user_from
            LEFT JOIN file f ON a.id_thumbnail = f.id
            WHERE s.id_user_to = :userId AND s.is_accepted = true
              AND a.visibility_state <> 'private'
            ORDER BY s.created_at
            """)
    List<InternalShareArtIDExtendedResponse> getInternalSharesToUserID(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            DELETE FROM internal_share s
            WHERE s.id IN (:shareIds) AND s.id_user_from = :userId
            """)
    void deleteSharesByIds(@Param("userId") Long userId, @Param("shareIds") List<Long> shareIds);

    Optional<InternalShare> findByIdUserFromAndIdUserToAndIdArtid(Long idUserFrom, Long idUserTo, Long idArtid);

    Optional<InternalShare> findByIdUserFromAndRecipientMailAndIdArtid(Long idUserFrom, String email, Long idArtid);

    @Modifying
    @Query("""
            UPDATE internal_share
            SET is_accepted = false
            WHERE id_user_to = :idUserTo
              AND id_artid = :idArtid
            """)
    int declineInternalShare(
            @Param("idUserTo") Long idUserTo,
            @Param("idArtid") Long idArtid);

    @Query("SELECT COUNT(*) > 0 FROM internal_share WHERE id_artid = :artidId")
    boolean existsActiveInternalShare(@Param("artidId") Long artidId);

    // Autorizza la vista dettaglio di un ArtID condiviso: vero se l'ArtID è condiviso (e accettato)
    // con l'utente destinatario. Consente l'accesso a prescindere dalla visibilità dell'ArtID.
    boolean existsByIdArtidAndIdUserToAndIsAcceptedTrue(Long idArtid, Long idUserTo);
}
