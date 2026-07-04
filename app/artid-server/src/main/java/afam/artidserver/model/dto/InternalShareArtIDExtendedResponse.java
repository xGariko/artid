package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record InternalShareArtIDExtendedResponse(
        Long id,
        Long idUserFrom,
        Long idUserTo,
        Long idArtid,
        String recipientMail,
        Boolean isAccepted,
        OffsetDateTime createdAt,
        String title,
        String filePath,
        String name
) {
}
