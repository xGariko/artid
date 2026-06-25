package afam.artidserver.model.dto;

/**
 * Materiale (risorsa) collegato a un ArtID pubblico, nella vista Explore. Espone i metadati e un
 * presigned URL allo stream del file: il client carica i byte solo al click (audio/video con
 * preload="none", video/immagini con click-to-load). {@code url} è null se il materiale non ha file.
 */
public record PublicMaterialResponse(
        Long id,
        String title,
        String description,
        String mimeType,
        String fileName,
        Long fileSize,
        String url
) {
}
