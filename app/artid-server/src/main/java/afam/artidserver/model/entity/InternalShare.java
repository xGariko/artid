package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("internal_share")
public class InternalShare {

    @Column("id_user")
    private Long idUser;

    @Column("id")
    private Long idArtid;
}
