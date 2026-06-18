package afam.artidserver.model.dto;

import lombok.Data;

/**
 * Step 2 dell'autenticazione SPID, solo nel ramo "email già registrata senza spidCode": l'OTP
 * inviato all'email dell'account esistente. {@code username} (codice fiscale) ri-risolve l'identità
 * mock per il merge dei dati; {@code email} individua l'account; {@code code} è l'OTP.
 */
@Data
public class SpidVerifyOtpRequest {
    private String username;
    private String email;
    private String code;
}
