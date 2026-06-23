package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record ExternalShareArtIDResponse(
        Long id,
        Long idArtid,
        Long clickCounter,
        Boolean isActive,
        OffsetDateTime expirationDate,
        OffsetDateTime lastOpened,
        OffsetDateTime createdAt,
        String title,
        Long idThumbnail
) {

}
