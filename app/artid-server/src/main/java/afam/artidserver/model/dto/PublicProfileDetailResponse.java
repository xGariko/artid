package afam.artidserver.model.dto;

import java.util.List;

/**
 * Read-model del dettaglio di un profilo PUBBLICO (pagina Explore → /explore/[id]).
 * Restituito solo se l'utente è pubblico e non eliminato. Espone esclusivamente dati
 * non sensibili e contenuti marcati pubblici: certificazioni (is_public) e ArtID (visibility_state = 'public').
 * Campi privati come mail di login, telefono, indirizzo, data di nascita NON compaiono.
 */
public record PublicProfileDetailResponse(
        Long id,
        String name,
        String surname,
        String profession,
        String location,
        String avatarUrl,
        // true se l'identità è verificata via SPID (spid_code valorizzato).
        boolean verified,
        String linkedinId,
        String facebookId,
        String instagramId,
        // Email di contatto (colonna business_email): mostrata tra i contatti del profilo pubblico.
        String businessEmail,
        // Biografia (rich-text): mostrata SOLO nella pagina del profilo pubblico (Explore), non nelle
        // viste ArtID.
        String biography,
        List<PublicCertificationResponse> certifications,
        List<PublicArtidSummaryResponse> artids
) {
}
