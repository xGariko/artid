package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Registrazione PENDENTE in attesa di verifica email (OTP). Conserva i dati del form di
 * registrazione e l'OTP finché il codice non viene verificato: solo allora si crea l'utente vero
 * ({@code afam.artidserver.service.RegistrationService}) e la riga viene consumata. Un solo pending
 * per email (indice UNIQUE su email): un nuovo tentativo sostituisce il precedente. Password e
 * codice non sono mai in chiaro (hash BCrypt).
 */
@Data
@Table("registration_otp")
public class RegistrationOtp {

    @Id
    private Long id;

    private String email;
    private String name;
    private String surname;

    @Column("password_hash")
    private String passwordHash;

    private LocalDate birthdate;
    private String birthplace;

    @Column("code_hash")
    private String codeHash;

    @Column("expires_at")
    private OffsetDateTime expiresAt;

    private Integer attempts;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
