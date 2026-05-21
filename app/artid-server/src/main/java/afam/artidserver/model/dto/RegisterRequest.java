package afam.artidserver.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    private String name;
    private String surname;
    private String email;
    private String password;
    private LocalDate birthdate;
    private String birthplace;
    private String taxId;
}
