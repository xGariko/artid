package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

/**
 * Risposta dello step 1 dell'autenticazione SPID. È un envelope unico perché l'endpoint può
 * concludere in due modi (vedi {@link Outcome}):
 * <ul>
 *   <li>{@code AUTHENTICATED}: utente trovato/creato e sessione rilasciata → {@code token} valorizzato;</li>
 *   <li>{@code OTP_REQUIRED}: l'email è già di un account esistente senza spidCode → serve la
 *       verifica OTP. La challenge è armata ma la mail NON è ancora partita: il client mostra prima
 *       la conferma d'invio (RAD AUT_MEM_ID §8.3.3.1) e l'OTP viene spedito solo all'"Ok". Per
 *       questo {@code expiresAt} è {@code null} qui (lo valorizza l'invio effettivo via resend).</li>
 * </ul>
 * Il client tipizzato discrimina sull'{@code outcome}.
 */
public record SpidAuthResponse(
        Outcome outcome,
        String token,
        Long id,
        String email,
        String name,
        String surname,
        OffsetDateTime expiresAt
) {
    public enum Outcome {
        AUTHENTICATED,
        OTP_REQUIRED
    }

    public static SpidAuthResponse authenticated(String token, Long id, String email, String name, String surname) {
        return new SpidAuthResponse(Outcome.AUTHENTICATED, token, id, email, name, surname, null);
    }

    public static SpidAuthResponse otpRequired(String email, OffsetDateTime expiresAt) {
        return new SpidAuthResponse(Outcome.OTP_REQUIRED, null, null, email, null, null, expiresAt);
    }
}
