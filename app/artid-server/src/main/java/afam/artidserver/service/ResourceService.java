package afam.artidserver.service;

import afam.artidserver.dao.FileDAO;
import afam.artidserver.dao.ResourceDAO;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.entity.File;
import afam.artidserver.model.entity.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceDAO resourceDAO;
    private final FileDAO fileDAO;

    public long countByUser(Long userId) {
        return resourceDAO.countByIdUserAndDeletedAtIsNull(userId);
    }

    public List<ResourceResponse> findByUser(Long userId) {
        List<Resource> resources = resourceDAO.findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(userId);

        List<Long> fileIds = resources.stream()
                .map(Resource::getIdFile)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, File> filesById = fileIds.isEmpty()
                ? Map.of()
                : fileDAO.findAllById(fileIds).stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));

        return resources.stream()
                .map(r -> {
                    File f = r.getIdFile() != null ? filesById.get(r.getIdFile()) : null;
                    Long size = (f != null && f.getBlob() != null) ? (long) f.getBlob().length : null;
                    return new ResourceResponse(
                            r.getId(),
                            r.getIdUser(),
                            r.getIdFile(),
                            r.getTitle(),
                            r.getDescription(),
                            r.getFavorite(),
                            r.getCreatedAt(),
                            r.getLastModified(),
                            f != null ? f.getFileName() : null,
                            f != null ? f.getExtension() : null,
                            f != null ? f.getMimeType() : null,
                            size,
                            0L
                    );
                })
                .toList();
    }

    public Optional<File> findFileByResourceId(Long resourceId, Long userId) {
        return resourceDAO.findById(resourceId)
                .filter(r -> userId.equals(r.getIdUser()) && r.getDeletedAt() == null && r.getIdFile() != null)
                .flatMap(r -> fileDAO.findById(r.getIdFile()));
    }
}
