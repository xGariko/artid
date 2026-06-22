package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record ArtidResponse(
        Long id,
        Long idUser,
        String title,
        String description,
        Boolean favourite,
        OffsetDateTime createdAt,
        OffsetDateTime lastModified
) {
}
