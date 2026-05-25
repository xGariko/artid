package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record ResourceResponse(
        Long id,
        Long idUser,
        Long idFile,
        String title,
        String description,
        Boolean favorite,
        OffsetDateTime createdAt,
        OffsetDateTime lastModified,
        String fileName,
        String extension,
        String mimeType,
        Long fileSize,
        Long artidCount
) {
}
