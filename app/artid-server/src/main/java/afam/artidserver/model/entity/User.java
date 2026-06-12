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

    private byte[] propic;

    @Column("internal_share_enabled")
    private Boolean internalShareEnabled;
}
