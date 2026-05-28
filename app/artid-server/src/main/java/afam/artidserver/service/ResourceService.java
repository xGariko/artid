package afam.artidserver.service;

import afam.artidserver.dao.FileDAO;
import afam.artidserver.dao.ResourceDAO;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.dto.ResourceUpsertRequest;
import afam.artidserver.model.entity.File;
import afam.artidserver.model.entity.Resource;
import afam.artidserver.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    // objectKey atteso: UUID v4 + estensione opzionale. Bocca tutto il resto per prevenire
    // path-traversal o key arbitrarie verso bucket non nostri (la firma del presigned non basta
    // come difesa perché il client potrebbe inventarsi un objectKey diverso da quello che gli
    // abbiamo dato in /upload-intent).
    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\\.[A-Za-z0-9]{1,16})?$"
    );

    private final ResourceDAO resourceDAO;
    private final FileDAO fileDAO;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ObjectStorageService objectStorageService;

    // Proiezione lightweight per le letture: niente blob in memoria, solo metadati + dimensione
    // calcolata via OCTET_LENGTH lato Postgres (null per i file su MinIO finché non aggiungiamo
    // colonna file_size persistita).
    private record FileMetadata(Long id, String fileName, String extension, String mimeType, Long fileSize) {}

    public long countByUser(Long userId) {
        return resourceDAO.countByIdUserAndDeletedAtIsNull(userId);
    }

    public List<ResourceResponse> findByUser(Long userId) {
        List<Resource> resources = resourceDAO.findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(userId);

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

    public Optional<File> findFileByResourceId(Long resourceId, Long userId) {
        return resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()) && r.getDeletedAt() == null && r.getIdFile() != null)
                .flatMap(r -> fileDAO.findById(r.getIdFile()));
    }

    @Transactional
    public ResourceResponse create(ResourceUpsertRequest request, Long userId) {
        if (request.objectKey() == null || request.objectKey().isBlank()) {
            throw new IllegalArgumentException("objectKey è obbligatorio per la creazione");
        }
        File savedFile = saveFile(request.fileName(), request.mimeType(), request.objectKey());

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
            linkArtidResource(saved.getId(), request.artidId());
        }
        return toResponse(saved, fileMetaOf(savedFile));
    }

    @Transactional
    public Optional<ResourceResponse> update(Long resourceId, ResourceUpsertRequest request, Long userId) {
        Optional<Resource> existing = resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()) && r.getDeletedAt() == null);
        if (existing.isEmpty()) return Optional.empty();

        Resource resource = existing.get();
        resource.setTitle(request.title());
        resource.setDescription(request.description());
        resource.setFavorite(Boolean.TRUE.equals(request.favorite()));
        resource.setLastModified(OffsetDateTime.now());

        // Se il file viene sostituito teniamo da parte il vecchio id e lo cancelliamo solo
        // DOPO aver salvato resource col nuovo id_file. Altrimenti la FK resource→file
        // punta ancora al vecchio record e Postgres rifiuta il delete.
        FileMetadata fileMeta;
        Long oldFileIdToDelete = null;
        String oldObjectKeyToDelete = null;
        if (request.objectKey() != null && !request.objectKey().isBlank()) {
            oldFileIdToDelete = resource.getIdFile();
            if (oldFileIdToDelete != null) {
                oldObjectKeyToDelete = fileDAO.findById(oldFileIdToDelete)
                        .map(File::getFilePath).orElse(null);
            }
            File newFile = saveFile(request.fileName(), request.mimeType(), request.objectKey());
            resource.setIdFile(newFile.getId());
            fileMeta = fileMetaOf(newFile);
        } else {
            fileMeta = resource.getIdFile() != null
                    ? findFilesMetadata(List.of(resource.getIdFile())).stream().findFirst().orElse(null)
                    : null;
        }
        Resource saved = resourceDAO.save(resource);

        // Il design corrente vincola un materiale a un solo ArtID → wipe+insert.
        jdbcTemplate.update("DELETE FROM artid_resource WHERE id_resource = ?", saved.getId());
        if (request.artidId() != null) {
            linkArtidResource(saved.getId(), request.artidId());
        }

        if (oldFileIdToDelete != null) {
            fileDAO.deleteById(oldFileIdToDelete);
            // L'object delete su MinIO va fuori dalla transazione DB e dopo il delete del File:
            // se MinIO fallisse la transazione DB è già committata. La delete è idempotente
            // lato ObjectStorageService (NoSuchKey silenziato).
            if (oldObjectKeyToDelete != null) {
                tryDeleteObject(oldObjectKeyToDelete);
            }
        }

        return Optional.of(toResponse(saved, fileMeta));
    }

    @Transactional
    public boolean delete(Long resourceId, Long userId) {
        return resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()))
                .map(r -> {
                    String objectKeyToDelete = null;
                    if (r.getIdFile() != null) {
                        objectKeyToDelete = fileDAO.findById(r.getIdFile())
                                .map(File::getFilePath).orElse(null);
                    }
                    // FK su artid_resource non ha ON DELETE CASCADE → pulizia manuale.
                    jdbcTemplate.update("DELETE FROM artid_resource WHERE id_resource = ?", r.getId());
                    resourceDAO.deleteById(r.getId());
                    if (r.getIdFile() != null) {
                        fileDAO.deleteById(r.getIdFile());
                    }
                    if (objectKeyToDelete != null) {
                        tryDeleteObject(objectKeyToDelete);
                    }
                    return true;
                })
                .orElse(false);
    }

    private List<FileMetadata> findFilesMetadata(List<Long> ids) {
        return namedJdbcTemplate.query(
                "SELECT id, file_name, extension, mime_type, OCTET_LENGTH(blob) AS file_size FROM file WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                (rs, rowNum) -> new FileMetadata(
                        rs.getLong("id"),
                        rs.getString("file_name"),
                        rs.getString("extension"),
                        rs.getString("mime_type"),
                        rs.getObject("file_size") != null ? rs.getLong("file_size") : null
                )
        );
    }

    private File saveFile(String fileName, String mimeType, String objectKey) {
        if (!OBJECT_KEY_PATTERN.matcher(objectKey).matches()) {
            throw new IllegalArgumentException("objectKey non valido");
        }
        // Verifica che l'oggetto esista davvero su MinIO. Senza questo check il client potrebbe
        // passare una key arbitraria e creare una Resource che punta al vuoto.
        ObjectStorageService.ObjectStat stat;
        try {
            stat = objectStorageService.stat(objectKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("Oggetto non trovato su storage: " + objectKey, e);
        }

        File file = new File();
        file.setFileName(fileName);
        file.setMimeType(mimeType != null ? mimeType : stat.contentType());
        file.setExtension(extractExtension(fileName));
        file.setFilePath(objectKey);
        file.setBlob(null);
        return fileDAO.save(file);
    }

    private void tryDeleteObject(String objectKey) {
        try {
            objectStorageService.delete(objectKey);
        } catch (Exception e) {
            // Object già rimosso o MinIO momentaneamente irraggiungibile: non rolliamo back la
            // transazione DB già committata. Lasciamo un "leak" sul bucket recuperabile da una
            // GC offline che incrocia file.file_path con la lista oggetti del bucket.
        }
    }

    private void linkArtidResource(Long resourceId, Long artidId) {
        jdbcTemplate.update(
                "INSERT INTO artid_resource (id_resource, id, rank) VALUES (?, ?, 0)",
                resourceId, artidId
        );
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
    }

    private FileMetadata fileMetaOf(File f) {
        Long size = f.getBlob() != null ? (long) f.getBlob().length : null;
        return new FileMetadata(f.getId(), f.getFileName(), f.getExtension(), f.getMimeType(), size);
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
                0L
        );
    }
}
