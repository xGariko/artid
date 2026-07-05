package afam.artidserver.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ArtidResponse(
                Long id,
                Long idUser,
                Long idThumbnail,
                String title,
                String description,
                Boolean favourite,
                String visibilityState,
                OffsetDateTime createdAt,
                OffsetDateTime lastModified,
                String thumbnailUrl,
                List<Long> tagIds) {
}
