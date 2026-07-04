package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Table("internal_share")
public class InternalShare {

    @Id
    private Long id;

    @Column("id_user_from")
    private Long idUserFrom;

    @Column("id_user_to")
    private Long idUserTo;

    @Column("id_artid")
    private Long idArtid;

    @Column("recipient_mail")
    private String recipientMail;

    @Column("is_accepted")
    private Boolean isAccepted;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
