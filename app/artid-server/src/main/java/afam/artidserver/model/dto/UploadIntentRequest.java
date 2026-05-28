package afam.artidserver.model.dto;

public record UploadIntentRequest(
        String fileName,
        String mimeType
) {
}
