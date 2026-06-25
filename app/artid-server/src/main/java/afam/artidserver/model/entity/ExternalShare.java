package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Table("external_share")
public class ExternalShare {

    @Id
    private Long id;

    @Column("id_artid")
    private Long idArtid;

    @Column("click_counter")
    private Integer clickCounter;

    @Column("is_active")
    private Boolean isActive;

    @Column("expiration_date")
    private OffsetDateTime expirationDate;

    @Column("last_opened")
    private OffsetDateTime lastOpened;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
