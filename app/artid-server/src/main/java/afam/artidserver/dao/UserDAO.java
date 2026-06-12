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
     * Esclude di proposito la colonna {@code propic} (immagine profilo in BYTEA): caricarla a
     * ogni richiesta significherebbe trascinare l'immagine dal DB remoto anche per un semplice
     * count. Le colonne non selezionate restano null sull'entity; l'immagine si carica
     * esplicitamente solo dove serve davvero (pagina profilo). "user" è parola riservata → quotata.
     */
    @Query("""
            SELECT id, name, surname, mail, password_hash, birthdate, birthplace, address,
                   spid_code, biography, linkedin_id, profession, is_public, phone,
                   deleted_at, internal_share_enabled
            FROM "user"
            WHERE mail = :mail
            """)
    Optional<User> findByMailForAuth(@Param("mail") String mail);
}
