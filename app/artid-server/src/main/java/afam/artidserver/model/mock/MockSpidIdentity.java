package afam.artidserver.model.mock;

import java.time.LocalDate;

/**
 * Identità SPID FINTA servita dal provider mockato
 * {@link afam.artidserver.service.MockSpidIdentityProvider}. Rappresenta le "informazioni personali"
 * che un vero Identity Provider SPID restituirebbe dopo l'autenticazione (RAD, caso d'uso AUT_MEM_ID:
 * Nome, Cognome, Email, Codice Fiscale, Data di nascita, Luogo di nascita).
 *
 * <p>La password è in chiaro: è un dato di mock per i test, NON una credenziale reale. Lo
 * {@code username} è il codice fiscale, usato sia come credenziale di login sia come {@code spidCode}.</p>
 */
public record MockSpidIdentity(
        String username,
        String password,
        String name,
        String surname,
        String email,
        LocalDate birthdate,
        String birthplace
) {
}
