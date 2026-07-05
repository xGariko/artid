package afam.artidserver.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Table("user")
public class User {

    @Id
    private Long id;

    private String name;
    private String surname;
    private String mail;

    @JsonIgnore
    @Column("password_hash")
    private String passwordHash;

    // true = l'account ha una password reale scelta dall'utente (registrazione classica, o Membro
    // SPID che l'ha poi impostata via "Modifica Password"). false = nato da SPID e mai dotato di una
    // password vera: password_hash è solo un valore casuale/inutilizzabile (vedi SpidAuthService).
    // Guida l'eliminazione account: password vs credenziali SPID. Non derivabile dai soli dati.
    @Column("password_set")
    private Boolean passwordSet;

    private LocalDate birthdate;
    private String birthplace;
    private String address;

    @Column("spid_code")
    private String spidCode;

    private String biography;

    @Column("linkedin_id")
    private String linkedinId;

    @Column("facebook_id")
    private String facebookId;

    @Column("instagram_id")
    private String instagramId;

    private String profession;

    @Column("is_public")
    private Boolean isPublic;

    private String phone;

    @Column("business_email")
    private String businessEmail;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;

    // Object key dell'immagine profilo sul bucket privato Supabase "propics" (non i byte:
    // quelli vivono su Storage). null = nessuna foto. L'URL si ottiene firmando la key.
    @Column("propic_path")
    private String propicPath;

    @Column("internal_share_enabled")
    private Boolean internalShareEnabled;
}
