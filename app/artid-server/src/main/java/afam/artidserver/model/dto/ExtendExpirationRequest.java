package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

public record ExtendExpirationRequest(
        OffsetDateTime expirationDate) {
}
