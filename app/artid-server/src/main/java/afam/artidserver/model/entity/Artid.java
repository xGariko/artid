package afam.artidserver.model.entity;

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

    @Column("id_tag")
    private Long idTag;

    @Column("id_visibility_state")
    private Long idVisibilityState;

    private String title;

    private String description;

    private Boolean favourite;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("last_modified")
    private OffsetDateTime lastModified;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;

    @Column("is_public")
    private Boolean isPublic;

    @Column("is_private")
    private Boolean isPrivate;
}
