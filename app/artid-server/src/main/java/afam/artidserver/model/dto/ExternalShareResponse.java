package afam.artidserver.model.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.OffsetDateTime;

public record ExternalShareResponse (
        Long id,
        Long idArtid,
        Long idUser,
        Integer clickCounter,
        Boolean isActive,
        OffsetDateTime expirationDate,
        OffsetDateTime lastOpened,
        OffsetDateTime createdAt,
        OffsetDateTime firstOpened,
        String description
) {
}
