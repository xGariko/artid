package afam.artidserver.storage;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ObjectStorageService {

    private final MinioClient internalClient;
    private final MinioClient publicClient;
    private final String bucket;
    private final int presignedTtlSeconds;

    public ObjectStorageService(
            MinioClient internalClient,
            @Qualifier("publicMinioClient") MinioClient publicClient,
            @Value("${minio.bucket}") String bucket,
            @Value("${minio.presigned-url-ttl-seconds}") int presignedTtlSeconds
    ) {
        this.internalClient = internalClient;
        this.publicClient = publicClient;
        this.bucket = bucket;
        this.presignedTtlSeconds = presignedTtlSeconds;
    }

    public String newObjectKey(String fileName) {
        String ext = extensionOf(fileName);
        String uuid = UUID.randomUUID().toString();
        return ext != null ? uuid + "." + ext : uuid;
    }

    // PUT presigned per upload diretto dal browser. Il browser fa PUT al publicEndpoint, MinIO
    // verifica la firma, accetta il body. Il backend non vede mai i bytes.
    public PresignedUpload presignedPut(String objectKey, String contentType) throws StorageException {
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .method(Method.PUT)
                    .expiry(presignedTtlSeconds, TimeUnit.SECONDS);
            if (contentType != null && !contentType.isBlank()) {
                builder.extraHeaders(java.util.Map.of("Content-Type", contentType));
            }
            String url = publicClient.getPresignedObjectUrl(builder.build());
            return new PresignedUpload(url, objectKey, presignedTtlSeconds);
        } catch (Exception e) {
            throw new StorageException("Errore generazione presigned URL", e);
        }
    }

    public void put(String objectKey, InputStream content, long size, String contentType) throws StorageException {
        try {
            internalClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, size, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
        } catch (Exception e) {
            throw new StorageException("Errore upload oggetto", e);
        }
    }

    public InputStream get(String objectKey) throws StorageException {
        try {
            return internalClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Errore lettura oggetto " + objectKey, e);
        }
    }

    public ObjectStat stat(String objectKey) throws StorageException {
        try {
            StatObjectResponse r = internalClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            return new ObjectStat(r.size(), r.contentType());
        } catch (Exception e) {
            throw new StorageException("Errore stat oggetto " + objectKey, e);
        }
    }

    // Non solleva se l'oggetto non esiste: la delete deve essere idempotente perché può venir
    // chiamata dopo che la transazione DB ha già rimosso il record File ma l'oggetto MinIO era
    // già assente (es. presigned scaduto senza upload effettivo).
    public void delete(String objectKey) throws StorageException {
        try {
            internalClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (ErrorResponseException e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                throw new StorageException("Errore delete oggetto " + objectKey, e);
            }
        } catch (Exception e) {
            throw new StorageException("Errore delete oggetto " + objectKey, e);
        }
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }

    public record PresignedUpload(String url, String objectKey, int expiresInSeconds) {}

    public record ObjectStat(long size, String contentType) {}

    public static class StorageException extends IOException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
