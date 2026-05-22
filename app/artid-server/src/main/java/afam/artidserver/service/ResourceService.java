package afam.artidserver.service;

import afam.artidserver.dao.ResourceDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceDAO resourceDAO;

    public long countByUser(Long userId) {
        return resourceDAO.countByIdUserAndDeletedAtIsNull(userId);
    }
}
