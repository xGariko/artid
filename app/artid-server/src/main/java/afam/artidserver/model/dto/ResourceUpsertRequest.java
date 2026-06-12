package afam.artidserver.model.dto;

/**
 * Metadati di una risorsa, inviati come campi form-data. Il file vero e proprio viaggia
 * come part {@code file} separata (multipart/form-data), non dentro questo oggetto.
 */
public record ResourceUpsertRequest(
        String title,
        String description,
        Boolean favorite,
        Long artidId
) {
}
