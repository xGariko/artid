package afam.artidserver.model.dto;

import lombok.Data;

/**
 * Step 1 dell'autenticazione SPID: credenziali inserite nella schermata del provider (mock).
 * {@code providerId} è il provider scelto a video ed è puramente cosmetico (qualsiasi provider
 * autentica qualsiasi identità); {@code username} è il codice fiscale.
 */
@Data
public class SpidLoginRequest {
    private String providerId;
    private String username;
    private String password;
}
