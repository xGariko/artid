package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record CreateExternalShareRequest(
        Long artidId,
        OffsetDateTime expirationDate,
        String description) {
}
