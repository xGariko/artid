package afam.artidserver.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Client S3 puntato all'endpoint S3-compatibile di Supabase Storage
 * (https://<project-ref>.supabase.co/storage/v1/s3).
 * <p>
 * Note importanti per Supabase:
 * <ul>
 *   <li>{@code pathStyleAccessEnabled(true)}: Supabase NON supporta il virtual-hosted style
 *       (bucket come sottodominio), quindi il bucket deve stare nel path.</li>
 *   <li>{@code region}: deve combaciare con la region del progetto (es. eu-west-1); l'SDK la usa
 *       per firmare la richiesta SigV4, Supabase la valida.</li>
 *   <li>Le credenziali sono le S3 access key generate da Supabase (Storage → S3 Connection),
 *       NON la service-role key.</li>
 * </ul>
 */
@Configuration
public class S3Config {

    // @Lazy: il client viene costruito al primo utilizzo, non all'avvio. Così l'app parte anche
    // se le credenziali S3 non sono ancora configurate (es. in CI o sviluppo di altre feature);
    // un eventuale errore di credenziali emerge solo al primo upload/download.
    @Bean
    @Lazy
    public S3Client s3Client(
            @Value("${supabase.s3.endpoint}") String endpoint,
            @Value("${supabase.s3.region}") String region,
            @Value("${supabase.s3.access-key}") String accessKey,
            @Value("${supabase.s3.secret-key}") String secretKey
    ) {
        if (isBlank(accessKey) || isBlank(secretKey)) {
            throw new IllegalStateException(
                    "Credenziali Supabase S3 mancanti: imposta supabase.s3.access-key e "
                            + "supabase.s3.secret-key in application-local.properties "
                            + "(Dashboard → Project Settings → Storage → S3 access keys).");
        }

        // AWS SDK 2.30+ abilita di default i checksum CRC32 e l'encoding 'aws-chunked' con trailer
        // sulle PUT. Gli endpoint S3-compatibili come Supabase li rifiutano (errori 400/501 sul
        // putObject). Riportiamo il comportamento a "solo quando richiesto" (come pre-2.30): va
        // impostato come system property PRIMA di build(), perché il client lo risolve in costruzione.
        System.setProperty("aws.requestChecksumCalculation", "when_required");
        System.setProperty("aws.responseChecksumValidation", "when_required");

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
