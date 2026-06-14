package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

/**
 * Sintesi di un ArtID pubblico nel dettaglio profilo (pagina Explore): titolo, data di
 * creazione e numero di risorse contenute. Solo gli ArtID con is_public sono inclusi.
 */
public record PublicArtidSummaryResponse(
        Long id,
        String title,
        OffsetDateTime createdAt,
        long resourceCount
) {
}
