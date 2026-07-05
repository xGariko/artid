package afam.artidserver.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Dettaglio di un ArtID PUBBLICO (Explore → /explore/[id]/artid/[artidId]). Restituito solo se
 * l'ArtID è 'public', non eliminato e appartiene a un utente pubblico (altrimenti 404, senza
 * distinguere i casi). Espone i dati dell'ArtID, un estratto dell'autore (header + contatti) e i
 * materiali collegati. Nessun campo personale sensibile dell'autore.
 */
public record PublicArtidDetailResponse(
        Long id,
        String title,
        String description,
        OffsetDateTime createdAt,
        String thumbnailUrl,
        Long authorId,
        String authorName,
        String authorSurname,
        String authorProfession,
        String authorAvatarUrl,
        // true se l'identità dell'autore è verificata via SPID.
        boolean authorVerified,
        // true se il profilo dell'autore è pubblico (is_public). Falso nei contesti di condivisione
        // in cui l'ArtID è visibile ma il profilo autore non lo è: il frontend disabilita "Vai al profilo".
        boolean authorProfilePublic,
        String authorLinkedinId,
        String authorFacebookId,
        String authorInstagramId,
        // Email di contatto dell'autore (colonna business_email).
        String authorBusinessEmail,
        List<PublicMaterialResponse> materials
) {
}
