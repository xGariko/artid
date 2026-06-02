package afam.artidserver.model.dto;

public record ResourceUpsertRequest(
        String title,
        String description,
        Boolean favorite,
        Long artidId,
        String fileName,
        String mimeType,
        // Contenuto file in base64. Obbligatorio in create; opzionale in update (se assente
        // si mantiene il file esistente).
        String fileContent
) {
}
