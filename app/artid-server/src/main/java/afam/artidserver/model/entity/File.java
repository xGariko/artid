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

    // Object key dell'oggetto su Supabase S3 (non un path filesystem).
    @Column("file_path")
    private String filePath;

    @Column("file_name")
    private String fileName;

    private String extension;

    @Column("mime_type")
    private String mimeType;

    // Dimensione in byte, salvata all'upload: con i byte ora su S3 non possiamo più
    // derivarla via OCTET_LENGTH(blob).
    @Column("file_size")
    private Long fileSize;
}
