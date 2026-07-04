package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record ExternalShareArtIDResponse(
        Long id,
        Long idArtid,
        Long idCreator,
        Integer clickCounter,
        Boolean isActive,
        OffsetDateTime expirationDate,
        OffsetDateTime lastOpened,
        OffsetDateTime createdAt,
        OffsetDateTime firstOpened,
        String description,
        String title,
        String file_path
) {

}
