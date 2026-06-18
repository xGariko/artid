package afam.artidserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Risposta dello step 1 del login: le credenziali sono valide e un OTP è stato inviato via
 * email. Non contiene token: la sessione viene rilasciata solo dopo la verifica del codice.
 */
@Data
@AllArgsConstructor
public class OtpChallengeResponse {
    private boolean otpRequired;
    private String email;
    private OffsetDateTime expiresAt;
}
