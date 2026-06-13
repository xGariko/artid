package afam.artidserver.service;

import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.dto.PublicProfileResponse;
import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDAO userRepository;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    // Ricerca "intelligente" del catalogo pubblico. Un utente è rilevante se è pubblico E
    // (matcha per nome/cognome/professione) OPPURE possiede almeno un ArtID PUBBLICO il cui
    // titolo matcha. Il conteggio mostrato è il totale dei suoi ArtID pubblici (non solo i match).
    // Ordinamento per rilevanza: prima i match su persona, poi su professione, infine solo-ArtID.
    // "user" è parola riservata → quotata.
    private static final String SEARCH_PUBLIC_PROFILES_SQL = """
            SELECT u.id, u.name, u.surname, u.profession,
                   (SELECT COUNT(*) FROM artid a
                     WHERE a.id_user = u.id AND a.is_public = TRUE AND a.deleted_at IS NULL
                   ) AS public_artid_count
            FROM "user" u
            WHERE u.is_public = TRUE
              AND u.deleted_at IS NULL
              AND (
                    :blankQuery = TRUE
                 OR u.name ILIKE :pattern
                 OR u.surname ILIKE :pattern
                 OR (COALESCE(u.name, '') || ' ' || COALESCE(u.surname, '')) ILIKE :pattern
                 OR u.profession ILIKE :pattern
                 OR EXISTS (
                        SELECT 1 FROM artid a
                         WHERE a.id_user = u.id AND a.is_public = TRUE AND a.deleted_at IS NULL
                           AND a.title ILIKE :pattern
                    )
              )
            ORDER BY
                CASE
                    WHEN u.name ILIKE :pattern
                      OR u.surname ILIKE :pattern
                      OR (COALESCE(u.name, '') || ' ' || COALESCE(u.surname, '')) ILIKE :pattern THEN 0
                    WHEN u.profession ILIKE :pattern THEN 1
                    ELSE 2
                END,
                u.surname, u.name
            """;

    private static final RowMapper<PublicProfileResponse> PUBLIC_PROFILE_ROW_MAPPER = (rs, rowNum) ->
            new PublicProfileResponse(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("surname"),
                    rs.getString("profession"),
                    rs.getLong("public_artid_count")
            );

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByMail(String mail) {
        return userRepository.findByMail(mail);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Cerca i profili PUBBLICI rilevanti per la query (vedi {@link #SEARCH_PUBLIC_PROFILES_SQL}).
     * Con query vuota restituisce l'intero catalogo pubblico (browse).
     */
    public List<PublicProfileResponse> searchPublicProfiles(String query) {
        String trimmed = query == null ? "" : query.trim();
        boolean blankQuery = trimmed.isEmpty();
        // Su query vuota '%' matcha tutti (catalogo completo). I metacaratteri LIKE digitati
        // dall'utente vengono neutralizzati con escape backslash (default Postgres).
        String pattern = blankQuery ? "%" : "%" + escapeLike(trimmed) + "%";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("pattern", pattern)
                .addValue("blankQuery", blankQuery);

        return namedJdbcTemplate.query(SEARCH_PUBLIC_PROFILES_SQL, params, PUBLIC_PROFILE_ROW_MAPPER);
    }

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
