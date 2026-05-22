package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Table("resource")
public class Resource {

    @Id
    private Long id;

    @Column("id_user")
    private Long idUser;

    @Column("id_file")
    private Long idFile;

    private String title;

    private String description;

    private Boolean favorite;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("last_modified")
    private OffsetDateTime lastModified;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;
}
