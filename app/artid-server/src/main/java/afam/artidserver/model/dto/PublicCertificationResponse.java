package afam.artidserver.model.dto;

/**
 * Attestato pubblico mostrato nel dettaglio di un profilo pubblico (pagina Explore).
 * Espone il titolo, l'estensione (per l'icona) e un presigned GET URL verso il bucket privato
 * "certifications": il browser scarica il file direttamente da Supabase senza passare dal backend
 * e senza autenticazione (l'URL è firmato e scade). {@code url} è null se l'attestato non ha file.
 */
public record PublicCertificationResponse(
        Long id,
        String title,
        String extension,
        String url
) {
}
