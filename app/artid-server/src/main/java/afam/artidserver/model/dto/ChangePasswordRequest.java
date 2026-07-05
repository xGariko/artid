package afam.artidserver.model.dto;

import lombok.Data;

/** Richiesta dello step 2 di MODIFICA PASSWORD (RAD): codice OTP + nuova password scelta dal Membro. */
@Data
public class ChangePasswordRequest {
    private String code;
    private String newPassword;
}
