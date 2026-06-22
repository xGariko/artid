package afam.artidserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationResponse {
    private Long id;
    private String title;
    private String description;
    private boolean isPublic;
    private String extension;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
}