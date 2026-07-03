package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record ArtidResponse(
                Long id,
                Long idUser,
                Long idThumbnail,
                String title,
                String description,
                Boolean favourite,
                String visibilityState,
                OffsetDateTime createdAt,
                OffsetDateTime lastModified) {
}
