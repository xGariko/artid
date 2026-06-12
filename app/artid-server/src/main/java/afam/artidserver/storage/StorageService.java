package afam.artidserver.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

/**
 * Facciata sopra l'S3Client per il bucket delle risorse su Supabase Storage.
 * Tutte le operazioni lavorano su una "object key" opaca (vedi {@link #newObjectKey}).
 */
@Service
public class StorageService {

    private final S3Client s3;
    private final String bucket;

    public StorageService(@Lazy S3Client s3, @Value("${supabase.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
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

    public void upload(String objectKey, InputStream content, long size, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build(),
                RequestBody.fromInputStream(content, size));
    }

    public byte[] download(String objectKey) {
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build()).asByteArray();
    }

    /**
     * Delete idempotente: S3 non solleva se l'oggetto non esiste, quindi è sicuro chiamarla
     * anche dopo che il record DB è già sparito o per ripulire un upload mai confermato.
     */
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }
}
