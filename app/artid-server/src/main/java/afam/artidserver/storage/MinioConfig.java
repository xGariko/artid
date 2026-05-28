package afam.artidserver.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {

    @Bean
    @Primary
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    // Client separato che firma i presigned URL usando l'endpoint pubblico (quello che il
    // browser raggiunge via Caddy), non l'endpoint interno loopback usato dal backend.
    // Senza questo, il browser proverebbe a PUT su http://localhost:9010 → fallirebbe.
    @Bean("publicMinioClient")
    public MinioClient publicMinioClient(
            @Value("${minio.public-endpoint}") String publicEndpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
