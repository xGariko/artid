package afam.artidserver.service;

import afam.artidserver.dao.UserDAO;
import afam.artidserver.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

/**
 * Foto profilo su Supabase Storage (bucket privato "propics"). Il DB tiene solo la object key
 * (user.propic_path); i byte vivono su Storage. L'immagine si serve via presigned GET URL, così
 * il browser la scarica direttamente da Supabase senza far passare i byte dal backend.
 */
@Service
@RequiredArgsConstructor
public class AvatarService {

    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024; // 5MB

    private final StorageService storageService;
    private final UserDAO userDAO;
    private final JdbcTemplate jdbcTemplate;

    @Value("${supabase.s3.propics-bucket}")
    private String bucket;

    @Value("${supabase.s3.presign-ttl-seconds}")
    private long presignTtlSeconds;

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);

    /** Presigned GET URL per una object key già nota (es. ricavata da una riga già caricata); null se assente. */
    public String presignKey(String key) {
        if (key == null || key.isBlank()) return null;
        return storageService.presignGet(bucket, key, Duration.ofSeconds(presignTtlSeconds));
    }

    /**
     * Carica una nuova foto profilo sul bucket e aggiorna propic_path; ritorna il presigned URL
     * della nuova immagine. La vecchia (se c'era) viene rimossa best-effort dopo l'update DB.
     */
    public String upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File mancante");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il file deve essere un'immagine");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Immagine troppo grande (max 5MB)");
        }

        String oldKey = userDAO.findPropicPathById(userId).orElse(null);
        String newKey = storageService.newObjectKey(file.getOriginalFilename());
        try {
            storageService.upload(bucket, newKey, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Errore lettura immagine in upload", e);
        }

        try {
            jdbcTemplate.update("UPDATE \"user\" SET propic_path = ? WHERE id = ?", newKey, userId);
        } catch (RuntimeException e) {
            // Il DB non è stato aggiornato: non lasciare l'oggetto appena caricato come orfano.
            safeDelete(newKey);
            throw e;
        }

        if (oldKey != null && !oldKey.isBlank()) {
            safeDelete(oldKey);
        }
        return storageService.presignGet(bucket, newKey, Duration.ofSeconds(presignTtlSeconds));
    }

    /** Rimuove la foto profilo: azzera propic_path e cancella l'oggetto dal bucket. */
    public void delete(Long userId) {
        String key = userDAO.findPropicPathById(userId).orElse(null);
        jdbcTemplate.update("UPDATE \"user\" SET propic_path = NULL WHERE id = ?", userId);
        if (key != null && !key.isBlank()) {
            safeDelete(key);
        }
    }

    // La pulizia su Storage è best-effort: un fallimento non deve propagarsi (il DB è la fonte di verità).
    private void safeDelete(String objectKey) {
        try {
            storageService.delete(bucket, objectKey);
        } catch (RuntimeException e) {
            logger.warn("Impossibile eliminare l'avatar '{}': {}", objectKey, e.getMessage());
        }
    }
}
