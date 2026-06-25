package afam.artidserver.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Facciata sopra l'S3Client per Supabase Storage. Le operazioni lavorano su una "object key"
 * opaca (vedi {@link #newObjectKey}). Il bucket è parametrico: gli overload senza bucket usano
 * quello di default (risorse), il bucket esplicito serve agli altri usi (es. foto profilo).
 */
@Service
public class StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String defaultBucket;

    public StorageService(@Lazy S3Client s3,
                          @Lazy S3Presigner presigner,
                          @Value("${supabase.s3.bucket}") String defaultBucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.defaultBucket = defaultBucket;
    }

    /**
     * Genera una chiave univoca preservando l'estensione (utile per debug nel bucket).
     * La chiave è scollegata dal nome file originale: niente collisioni, niente path traversal.
     */
    public String newObjectKey(String fileName) {
        String ext = extensionOf(fileName);
        String uuid = UUID.randomUUID().toString();
        return ext != null ? uuid + "." + ext : uuid;
    }

    // --- Overload senza bucket: usano il bucket di default (risorse). ---

    public void upload(String objectKey, InputStream content, long size, String contentType) {
        upload(defaultBucket, objectKey, content, size, contentType);
    }

    public byte[] download(String objectKey) {
        return download(defaultBucket, objectKey);
    }

    public void delete(String objectKey) {
        delete(defaultBucket, objectKey);
    }

    public String presignGet(String objectKey, Duration ttl) {
        return presignGet(defaultBucket, objectKey, ttl);
    }

    // --- Overload con bucket esplicito. ---

    public void upload(String bucket, String objectKey, InputStream content, long size, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build(),
                RequestBody.fromInputStream(content, size));
    }

    public byte[] download(String bucket, String objectKey) {
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build()).asByteArray();
    }

    /**
     * Delete idempotente: S3 non solleva se l'oggetto non esiste, quindi è sicuro chiamarla
     * anche dopo che il record DB è già sparito o per ripulire un upload mai confermato.
     */
    public void delete(String bucket, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }

    /**
     * Genera un GET URL firmato (SigV4) per un oggetto in un bucket privato: il browser scarica
     * l'immagine direttamente da Supabase, i byte non passano dal backend. L'URL scade dopo {@code ttl}.
     */
    public String presignGet(String bucket, String objectKey, Duration ttl) {
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build())
                .build()).url().toString();
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }
}
