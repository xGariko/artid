package afam.artidserver.model.dto;

public record ResourceUpsertRequest(
        String title,
        String description,
        Boolean favorite,
        Long artidId,
        String fileName,
        String mimeType,
        // Chiave dell'oggetto MinIO già caricato via presigned PUT (vedi /upload-intent).
        // Obbligatorio in create; opzionale in update (se assente si mantiene il file esistente).
        String objectKey
) {
}
