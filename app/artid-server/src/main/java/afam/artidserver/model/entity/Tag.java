package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

 @Data
 @Table("tag")
 public class Tag {

  @Id
  private Long id;

  @Column("id_user")
  private Long idUser;

  private String title;

  private String color;

 }
