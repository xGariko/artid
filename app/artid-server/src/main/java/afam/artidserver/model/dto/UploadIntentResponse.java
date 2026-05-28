package afam.artidserver.model.dto;

public record UploadIntentResponse(
        String uploadUrl,
        String objectKey,
        int expiresInSeconds
) {
}
