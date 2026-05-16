package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("user")
public class User {

    @Id
    private Long id;
    private String email;
    private String username;
    private String password;
    private String role;
}
