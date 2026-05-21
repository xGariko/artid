package afam.artidserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String email;
    private String name;
    private String surname;
}
