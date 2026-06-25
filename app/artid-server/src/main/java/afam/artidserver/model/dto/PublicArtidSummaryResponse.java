package afam.artidserver.model.dto;

import java.time.OffsetDateTime;

/**
 * Sintesi di un ArtID pubblico nel dettaglio profilo (pagina Explore): titolo, data di
 * creazione, numero di risorse contenute e presigned URL della thumbnail (null se assente).
 * Solo gli ArtID con visibility_state = 'public' sono inclusi.
 */
public record PublicArtidSummaryResponse(
        Long id,
        String title,
        OffsetDateTime createdAt,
        long resourceCount,
        String thumbnailUrl
) {
}
