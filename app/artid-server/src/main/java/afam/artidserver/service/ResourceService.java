package afam.artidserver.service;

import afam.artidserver.dao.FileDAO;
import afam.artidserver.dao.ResourceDAO;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.dto.ResourceUpsertRequest;
import afam.artidserver.model.entity.File;
import afam.artidserver.model.entity.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Base64;
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
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    // Proiezione lightweight per le letture: niente blob in memoria, solo metadati + dimensione
    // calcolata via OCTET_LENGTH lato Postgres.
    private record FileMetadata(Long id, String fileName, String extension, String mimeType, Long fileSize) {
        static FileMetadata of(File f) {
            return new FileMetadata(
                    f.getId(),
                    f.getFileName(),
                    f.getExtension(),
                    f.getMimeType(),
                    f.getBlob() != null ? (long) f.getBlob().length : null
            );
        }
    }

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
        if (request.fileContent() == null || request.fileContent().isBlank()) {
            throw new IllegalArgumentException("fileContent è obbligatorio per la creazione");
        }
        File savedFile = saveFile(request.fileName(), request.mimeType(), request.fileContent());

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
        return toResponse(saved, FileMetadata.of(savedFile));
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
        if (request.fileContent() != null && !request.fileContent().isBlank()) {
            oldFileIdToDelete = resource.getIdFile();
            File newFile = saveFile(request.fileName(), request.mimeType(), request.fileContent());
            resource.setIdFile(newFile.getId());
            fileMeta = FileMetadata.of(newFile);
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
                        fileDAO.deleteById(r.getIdFile());
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

    private File saveFile(String fileName, String mimeType, String base64Content) {
        File file = new File();
        file.setFileName(fileName);
        file.setMimeType(mimeType);
        file.setExtension(extractExtension(fileName));
        file.setBlob(Base64.getDecoder().decode(base64Content));
        return fileDAO.save(file);
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
