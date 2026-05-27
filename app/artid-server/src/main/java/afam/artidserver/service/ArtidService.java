package afam.artidserver.service;

import afam.artidserver.dao.ArtidDAO;
import afam.artidserver.model.dto.ArtidResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtidService {

    private final ArtidDAO artidDAO;

    public long countByUser(Long userId) {
        return artidDAO.countByIdUserAndDeletedAtIsNull(userId);
    }

    public List<ArtidResponse> findByUser(Long userId) {
        return artidDAO.findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(userId)
                .stream()
                .map(a -> new ArtidResponse(
                        a.getId(),
                        a.getIdUser(),
                        a.getTitle(),
                        a.getFavourite(),
                        a.getCreatedAt(),
                        a.getLastModified(),
                        a.getIsPublic(),
                        a.getIsPrivate()
                ))
                .toList();
    }
}
