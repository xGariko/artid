package afam.artidserver.service;

import afam.artidserver.dao.FileDAO;
import afam.artidserver.dao.ResourceDAO;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.dto.ResourceUpsertRequest;
import afam.artidserver.model.entity.File;
import afam.artidserver.model.entity.Resource;
import afam.artidserver.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceDAO resourceDAO;
    private final FileDAO fileDAO;
    private final StorageService storageService;
    private final ArtidService artidService;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    // Proiezione lightweight per le letture: solo metadati, niente byte. La
    // dimensione ora
    // arriva dalla colonna file_size (i byte vivono su S3).
    private record FileMetadata(Long id, String fileName, String extension, String mimeType, Long fileSize) {
        static FileMetadata of(File f) {
            return new FileMetadata(f.getId(), f.getFileName(), f.getExtension(), f.getMimeType(), f.getFileSize());
        }
    }

    /** File scaricabile: metadati + contenuto recuperato da S3. */
    public record DownloadableFile(String fileName, String mimeType, byte[] content) {
    }

    public long countByUser(Long userId) {
        return resourceDAO.countByIdUserAndDeletedAtIsNull(userId);
    }

    public List<ResourceResponse> findByUser(Long userId) {
        return toResponses(resourceDAO.findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(userId));
    }

    /**
     * Materiali collegati a un ArtID. La proprietà è verificata nella query (vedi
     * {@link afam.artidserver.dao.ResourceDAO#findByArtidForUser}): se l'ArtID non
     * è dell'utente
     * loggato la lista è vuota, e comunque tornano solo materiali suoi → niente
     * accesso a dati altrui.
     */
    public List<ResourceResponse> findByArtid(Long artidId, Long userId) {
        return toResponses(resourceDAO.findByArtidForUser(artidId, userId));
    }

    // Mappa una lista di Resource in ResourceResponse, recuperando i metadati dei
    // file collegati
    // in un'unica query batch (niente N+1).
    private List<ResourceResponse> toResponses(List<Resource> resources) {
        List<Long> fileIds = resources.stream()
                .map(Resource::getIdFile)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, FileMetadata> metadataById = fileIds.isEmpty()
                ? Map.of()
                : findFilesMetadata(fileIds).stream()
                        .collect(Collectors.toMap(FileMetadata::id, Function.identity()));

        return resources.stream()
                .map(r -> toResponse(r, r.getIdFile() != null ? metadataById.get(r.getIdFile()) : null))
                .toList();
    }

    public Optional<DownloadableFile> findDownloadable(Long resourceId, Long userId) {
        return resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()) && r.getDeletedAt() == null && r.getIdFile() != null)
                .flatMap(r -> fileDAO.findById(r.getIdFile()))
                .filter(f -> f.getFilePath() != null)
                .map(f -> new DownloadableFile(f.getFileName(), f.getMimeType(),
                        storageService.download(f.getFilePath())));
    }

    @Transactional
    public ResourceResponse create(ResourceUpsertRequest request, MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Il file è obbligatorio per la creazione");
        }
        File savedFile = saveFile(file);

        Resource resource = new Resource();
        resource.setIdUser(userId);
        resource.setIdFile(savedFile.getId());
        resource.setTitle(request.title());
        resource.setDescription(request.description());
        resource.setFavorite(Boolean.TRUE.equals(request.favorite()));
        OffsetDateTime now = OffsetDateTime.now();
        resource.setCreatedAt(now);
        resource.setLastModified(now);
        Resource saved = resourceDAO.save(resource);

        if (request.artidId() != null) {
            artidService.linkArtidResource(request.artidId(), saved.getId(), userId);
        }
        return toResponse(saved, FileMetadata.of(savedFile));
    }

    @Transactional
    public Optional<ResourceResponse> update(Long resourceId, ResourceUpsertRequest request, MultipartFile file,
            Long userId) {
        Optional<Resource> existing = resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()) && r.getDeletedAt() == null);
        if (existing.isEmpty())
            return Optional.empty();

        Resource resource = existing.get();
        resource.setTitle(request.title());
        resource.setDescription(request.description());
        resource.setFavorite(Boolean.TRUE.equals(request.favorite()));
        resource.setLastModified(OffsetDateTime.now());

        // Se il file viene sostituito, teniamo da parte vecchio id e object key. Il
        // record File
        // vecchio va cancellato solo DOPO aver salvato resource col nuovo id_file,
        // altrimenti la
        // FK resource→file punta ancora al vecchio record e Postgres rifiuta il delete.
        // L'oggetto
        // su S3 lo eliminiamo solo a commit avvenuto: se la transazione fallisce il
        // file resta.
        FileMetadata fileMeta;
        Long oldFileIdToDelete = null;
        String oldObjectKeyToDelete = null;
        if (file != null && !file.isEmpty()) {
            oldFileIdToDelete = resource.getIdFile();
            oldObjectKeyToDelete = oldFileIdToDelete != null
                    ? fileDAO.findById(oldFileIdToDelete).map(File::getFilePath).orElse(null)
                    : null;
            File newFile = saveFile(file);
            resource.setIdFile(newFile.getId());
            fileMeta = FileMetadata.of(newFile);
        } else {
            fileMeta = resource.getIdFile() != null
                    ? findFilesMetadata(List.of(resource.getIdFile())).stream().findFirst().orElse(null)
                    : null;
        }
        Resource saved = resourceDAO.save(resource);

        // TODO rivedere logica
        // Il design corrente vincola un materiale a un solo ArtID → wipe+insert.
        jdbcTemplate.update("DELETE FROM artid_resource WHERE id_resource = ?", saved.getId());
        if (request.artidId() != null) {
            artidService.linkArtidResource(request.artidId(), saved.getId(), userId);
        }

        if (oldFileIdToDelete != null) {
            fileDAO.deleteById(oldFileIdToDelete);
            deleteObjectAfterCommit(oldObjectKeyToDelete);
        }

        return Optional.of(toResponse(saved, fileMeta));
    }

    @Transactional
    public boolean delete(Long resourceId, Long userId) {
        return resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()))
                .map(r -> {
                    // FK su artid_resource non ha ON DELETE CASCADE → pulizia manuale.
                    jdbcTemplate.update("DELETE FROM artid_resource WHERE id_resource = ?", r.getId());
                    resourceDAO.deleteById(r.getId());
                    if (r.getIdFile() != null) {
                        String objectKey = fileDAO.findById(r.getIdFile()).map(File::getFilePath).orElse(null);
                        fileDAO.deleteById(r.getIdFile());
                        deleteObjectAfterCommit(objectKey);
                    }
                    return true;
                })
                .orElse(false);
    }

    private List<FileMetadata> findFilesMetadata(List<Long> ids) {
        return namedJdbcTemplate.query(
                "SELECT id, file_name, extension, mime_type, file_size FROM file WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                (rs, rowNum) -> new FileMetadata(
                        rs.getLong("id"),
                        rs.getString("file_name"),
                        rs.getString("extension"),
                        rs.getString("mime_type"),
                        rs.getObject("file_size") != null ? rs.getLong("file_size") : null));
    }

    /**
     * Carica i byte su S3 e registra il record File con la object key. Se la
     * transazione
     * dovesse fare rollback, l'oggetto appena caricato viene rimosso per non
     * lasciare orfani.
     */
    private File saveFile(MultipartFile multipartFile) {
        String objectKey = storageService.newObjectKey(multipartFile.getOriginalFilename());
        try {
            storageService.upload(objectKey, multipartFile.getInputStream(),
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

    // Cancella l'oggetto S3 solo dopo il commit della transazione (file sostituito
    // o eliminato).
    private void deleteObjectAfterCommit(String objectKey) {
        if (objectKey == null)
            return;
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

    // Rimuove l'oggetto appena caricato se la transazione fa rollback (evita orfani
    // su S3).
    private void deleteObjectOnRollback(String objectKey) {
        if (objectKey == null || !TransactionSynchronizationManager.isSynchronizationActive())
            return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    safeDelete(objectKey);
                }
            }
        });
    }

    // La pulizia S3 è best-effort: un fallimento non deve propagarsi (il DB è già
    // committato).
    private void safeDelete(String objectKey) {
        try {
            storageService.delete(objectKey);
        } catch (RuntimeException e) {
            logger.warn("Impossibile eliminare l'oggetto S3 '{}': {}", objectKey, e.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null)
            return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }

    private ResourceResponse toResponse(Resource r, FileMetadata f) {
        return new ResourceResponse(
                r.getId(),
                r.getIdUser(),
                r.getIdFile(),
                r.getTitle(),
                r.getDescription(),
                r.getFavorite(),
                r.getCreatedAt(),
                r.getLastModified(),
                f != null ? f.fileName() : null,
                f != null ? f.extension() : null,
                f != null ? f.mimeType() : null,
                f != null ? f.fileSize() : null,
                0L);
    }
}
