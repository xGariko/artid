package afam.artidserver.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("file")
public class File {

    @Id
    private Long id;

    @Column("file_path")
    private String filePath;

    @Column("file_name")
    private String fileName;

    private String extension;

    @Column("mime_type")
    private String mimeType;

    private byte[] blob;
}
