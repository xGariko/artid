package afam.artidserver.model.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("artid_tag")
public class ArtidTag {

    @Id
    private Long id;

    @Column("id_artid")
    private Long idArtid;

    @Column("id_tag")
    private Long idTag;

}
