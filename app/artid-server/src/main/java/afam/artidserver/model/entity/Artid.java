package afam.artidserver.model.entity;

import afam.artidserver.model.VISIBILITY_STATE;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Table("artid")
public class Artid {

    @Id
    private Long id;

    @Column("id_user")
    private Long idUser;

    @Column("id_thumbnail")
    private Long idThumbnail;

    private String title;

    private String description;

    private Boolean favourite;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("last_modified")
    private OffsetDateTime lastModified;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;

    @Column("visibility_state")
    private VISIBILITY_STATE visibilityState;
}
