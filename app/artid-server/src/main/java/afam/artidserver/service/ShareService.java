package afam.artidserver.service;

import afam.artidserver.dao.ExternalShareDAO;
import afam.artidserver.dao.InternalShareDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ExternalShareDAO externalShareDAO;
    private final InternalShareDAO internalShareDAO;

    public long countByUser(Long userId) {
        return externalShareDAO.countByArtidOwner(userId) + internalShareDAO.countByArtidOwner(userId);
    }
}
