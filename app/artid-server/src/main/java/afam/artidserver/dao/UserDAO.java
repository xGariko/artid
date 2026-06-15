package afam.artidserver.dao;

import afam.artidserver.model.entity.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserDAO extends ListCrudRepository<User, Long> {

    Optional<User> findByMail(String mail);

    /**
     * Lookup di autenticazione: gira nell'hot path (filtro JWT) a OGNI richiesta autenticata.
     * Non seleziona {@code propic_path}: l'avatar si risolve a parte (vedi {@link #findPropicPathById}),
     * non serve nel principal. Le colonne non selezionate restano null sull'entity.
     * "user" è parola riservata → quotata.
     */
    @Query("""
            SELECT id, name, surname, mail, password_hash, birthdate, birthplace, address,
                   spid_code, biography, linkedin_id, facebook_id, instagram_id, profession,
                   is_public, phone, business_email, deleted_at, internal_share_enabled
            FROM "user"
            WHERE mail = :mail
            """)
    Optional<User> findByMailForAuth(@Param("mail") String mail);

    /**
     * Carica SOLO la object key dell'avatar (propic_path) di un utente. Query dedicata e
     * minimale per la navbar (presente su ogni pagina): evita di trascinare l'intera riga
     * utente dove serve solo l'immagine. "user" è parola riservata → quotata. null → Optional vuoto.
     */
    @Query("SELECT propic_path FROM \"user\" WHERE id = :id")
    Optional<String> findPropicPathById(@Param("id") Long id);
}
