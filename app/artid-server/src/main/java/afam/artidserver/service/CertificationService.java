package afam.artidserver.service;

import afam.artidserver.dao.CertificationDAO;
import afam.artidserver.dao.FileDAO;
import afam.artidserver.model.entity.Certification;
import afam.artidserver.model.entity.File;
import afam.artidserver.model.dto.CertificationResponse;
import afam.artidserver.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Attestati su Supabase Storage (bucket privato "certifications"). Come per i materiali
 * (vedi {@link ResourceService}) il DB tiene solo i metadati: i byte vivono su S3 e il record
 * {@link File} ne contiene la object key. Il download passa dal backend, che verifica l'ownership
 * prima di servire il file.
 */
@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationDAO certificationDAO;
    private final FileDAO fileDAO;
    private final StorageService storageService;

    @Value("${supabase.s3.certifications-bucket}")
    private String bucket;

    private static final Logger logger = LoggerFactory.getLogger(CertificationService.class);

    /** File scaricabile: metadati + contenuto recuperato da S3. */
    public record DownloadableFile(String fileName, String mimeType, byte[] content) {
    }

    public long countByUser(Long userId) {
        return certificationDAO.countByIdUser(userId);
    }

    // Recupera tutte le certificazioni dell'utente, con i metadati dei file collegati
    // in un'unica query batch (niente N+1).
    public List<CertificationResponse> findAllCertifications(Long userId) {
        List<Certification> certifications = certificationDAO.findAllByIdUser(userId);

        List<Long> fileIds = certifications.stream()
                .map(Certification::getIdFile)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, File> filesById = fileIds.isEmpty()
                ? Map.of()
                : fileDAO.findAllById(fileIds).stream()
                        .collect(Collectors.toMap(File::getId, Function.identity()));

        return certifications.stream()
                .map(c -> convertToResponse(c, c.getIdFile() != null ? filesById.get(c.getIdFile()) : null))
                .collect(Collectors.toList());
    }

    /**
     * Contenuto di un attestato per il download, solo se appartiene all'utente loggato.
     * I byte sono recuperati da S3 al momento della richiesta.
     */
    public Optional<DownloadableFile> findDownloadable(Long certificationId, Long userId) {
        return certificationDAO.findById(certificationId)
                .filter(c -> userId.equals(c.getIdUser()) && c.getIdFile() != null)
                .flatMap(c -> fileDAO.findById(c.getIdFile()))
                .filter(f -> f.getFilePath() != null)
                .map(f -> new DownloadableFile(f.getFileName(), f.getMimeType(),
                        storageService.download(bucket, f.getFilePath())));
    }

    /**
     * Carica il file su S3, registra il record {@link File} e crea la certificazione collegata.
     * Se la transazione fa rollback l'oggetto appena caricato viene rimosso (niente orfani su S3).
     */
    @Transactional
    public CertificationResponse saveCertification(String title, String description, Boolean isPublic,
            MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il file è obbligatorio per la certificazione");
        }
        File savedFile = saveFile(file);

        Certification certification = new Certification();
        certification.setIdUser(userId);
        certification.setIdFile(savedFile.getId());
        certification.setTitle(title);
        certification.setDescription(description);
        certification.setIsPublic(isPublic);

        Certification saved = certificationDAO.save(certification);
        return convertToResponse(saved, savedFile);
    }

    /**
     * Aggiorna una certificazione dell'utente. Se arriva un nuovo file lo carica su S3 e sostituisce
     * il precedente: il vecchio record {@link File} si cancella solo DOPO aver salvato la
     * certificazione col nuovo id_file (altrimenti la FK punta ancora al vecchio record e Postgres
     * rifiuta il delete), e l'oggetto su S3 si elimina solo a commit avvenuto.
     */
    @Transactional
    public Optional<CertificationResponse> updateCertification(Long id, String title, String description,
            Boolean isPublic, MultipartFile file, Long userId) {
        Optional<Certification> existing = certificationDAO.findById(id)
                .filter(c -> userId.equals(c.getIdUser()));
        if (existing.isEmpty()) return Optional.empty();

        Certification certification = existing.get();
        certification.setTitle(title);
        certification.setDescription(description);
        certification.setIsPublic(isPublic);

        File fileMeta;
        Long oldFileIdToDelete = null;
        String oldObjectKeyToDelete = null;
        if (file != null && !file.isEmpty()) {
            oldFileIdToDelete = certification.getIdFile();
            oldObjectKeyToDelete = oldFileIdToDelete != null
                    ? fileDAO.findById(oldFileIdToDelete).map(File::getFilePath).orElse(null)
                    : null;
            File newFile = saveFile(file);
            certification.setIdFile(newFile.getId());
            fileMeta = newFile;
        } else {
            fileMeta = certification.getIdFile() != null
                    ? fileDAO.findById(certification.getIdFile()).orElse(null)
                    : null;
        }

        Certification saved = certificationDAO.save(certification);

        if (oldFileIdToDelete != null) {
            fileDAO.deleteById(oldFileIdToDelete);
            deleteObjectAfterCommit(oldObjectKeyToDelete);
        }

        return Optional.of(convertToResponse(saved, fileMeta));
    }

    /** Elimina la certificazione dell'utente, il record File collegato e l'oggetto su S3. */
    @Transactional
    public boolean deleteCertification(Long id, Long userId) {
        return certificationDAO.findById(id)
                .filter(c -> userId.equals(c.getIdUser()))
                .map(c -> {
                    certificationDAO.deleteById(c.getId());
                    if (c.getIdFile() != null) {
                        String objectKey = fileDAO.findById(c.getIdFile()).map(File::getFilePath).orElse(null);
                        fileDAO.deleteById(c.getIdFile());
                        deleteObjectAfterCommit(objectKey);
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * Carica i byte su S3 (bucket attestati) e registra il record File con la object key.
     * Se la transazione dovesse fare rollback, l'oggetto appena caricato viene rimosso.
     */
    private File saveFile(MultipartFile multipartFile) {
        String objectKey = storageService.newObjectKey(multipartFile.getOriginalFilename());
        try {
            storageService.upload(bucket, objectKey, multipartFile.getInputStream(),
                    multipartFile.getSize(), multipartFile.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Errore lettura del file in upload", e);
        }
        deleteObjectOnRollback(objectKey);

        File file = new File();
        file.setFilePath(objectKey);
        file.setFileName(multipartFile.getOriginalFilename());
        file.setMimeType(multipartFile.getContentType());
        file.setExtension(extractExtension(multipartFile.getOriginalFilename()));
        file.setFileSize(multipartFile.getSize());
        return fileDAO.save(file);
    }

    // Cancella l'oggetto S3 solo dopo il commit della transazione (file sostituito o eliminato).
    private void deleteObjectAfterCommit(String objectKey) {
        if (objectKey == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDelete(objectKey);
            }
        });
    }

    // Rimuove l'oggetto appena caricato se la transazione fa rollback (evita orfani su S3).
    private void deleteObjectOnRollback(String objectKey) {
        if (objectKey == null || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    safeDelete(objectKey);
                }
            }
        });
    }

    // La pulizia S3 è best-effort: un fallimento non deve propagarsi (il DB è la fonte di verità).
    private void safeDelete(String objectKey) {
        try {
            storageService.delete(bucket, objectKey);
        } catch (RuntimeException e) {
            logger.warn("Impossibile eliminare l'oggetto S3 '{}': {}", objectKey, e.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }

    // Converte l'Entity nel DTO, ricavando dimensione ed estensione dal record File collegato.
    private CertificationResponse convertToResponse(Certification cert, File file) {
        return CertificationResponse.builder()
                .id(cert.getId())
                .title(cert.getTitle())
                .description(cert.getDescription())
                .isPublic(cert.getIsPublic() != null ? cert.getIsPublic() : false)
                .extension(file != null ? file.getExtension() : null)
                .fileSize(file != null ? file.getFileSize() : null)
                .build();
    }
}
