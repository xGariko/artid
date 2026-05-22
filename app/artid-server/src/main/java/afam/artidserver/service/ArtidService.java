package afam.artidserver.service;

import afam.artidserver.dao.ArtidDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtidService {

    private final ArtidDAO artidDAO;

    public long countByUser(Long userId) {
        return artidDAO.countByIdUserAndDeletedAtIsNull(userId);
    }
}
