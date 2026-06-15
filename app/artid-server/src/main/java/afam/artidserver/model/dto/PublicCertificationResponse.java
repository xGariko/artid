package afam.artidserver.model.dto;

/**
 * Attestato pubblico mostrato nel dettaglio di un profilo pubblico (pagina Explore).
 * Espone solo il titolo: il download del file non è previsto da questa vista.
 */
public record PublicCertificationResponse(
        Long id,
        String title
) {
}
