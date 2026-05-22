package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("certifications")
public class Certification {

    @Id
    private Long id;

    @Column("id_user")
    private Long idUser;

    @Column("id_file")
    private Long idFile;

    private String title;

    private String description;

    @Column("is_public")
    private Boolean isPublic;
}
