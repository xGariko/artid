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

    // Etichetta dell'enum Postgres visibility_state ('public'/'private'/'unlisted'). Tenuta come
    // String: in scrittura si passa per un CAST espicito (vedi ArtidService), in lettura il driver
    // restituisce già il label testuale.
    @Column("visibility_state")
    private String visibilityState;
}
