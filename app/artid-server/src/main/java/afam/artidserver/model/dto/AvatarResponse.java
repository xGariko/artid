package afam.artidserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Avatar dell'utente come presigned GET URL verso il bucket privato "propics"
 * (null se l'utente non ha foto). I byte non passano dal backend.
 */
@Data
@AllArgsConstructor
public class AvatarResponse {
    private String url;
}
