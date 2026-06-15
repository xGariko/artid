package afam.artidserver.model.dto;

/**
 * Read-model di un profilo pubblico nel catalogo di scoperta (pagina "Explore").
 * Espone solo dati non sensibili e il numero di ArtID pubblici dell'utente.
 */
public record PublicProfileResponse(
        Long id,
        String name,
        String surname,
        String profession,
        long publicArtidCount
) {
}
