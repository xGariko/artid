package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/**
 * Challenge OTP pendente per un utente: una riga viene creata SOLO dopo che le credenziali
 * sono state validate (vedi {@code AuthController.login}), poi consumata alla verifica.
 * Un solo OTP attivo per utente (indice UNIQUE su id_user): la rigenerazione "Riprova"
 * sostituisce la riga esistente. Il codice non viene salvato in chiaro ma come hash BCrypt.
 */
@Data
@Table("login_otp")
public class LoginOtp {

    @Id
    private Long id;

    @Column("id_user")
    private Long idUser;

    @Column("code_hash")
    private String codeHash;

    @Column("expires_at")
    private OffsetDateTime expiresAt;

    private Integer attempts;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
