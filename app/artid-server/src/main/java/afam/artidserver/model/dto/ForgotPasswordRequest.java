package afam.artidserver.model.dto;

import lombok.Data;

/** Richiesta dello step 1 del recupero password (RAD, caso d'uso DIM PASS): solo l'email. */
@Data
public class ForgotPasswordRequest {
    private String email;
}
