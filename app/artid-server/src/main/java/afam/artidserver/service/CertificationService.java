package afam.artidserver.service;

import afam.artidserver.dao.CertificationDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationDAO certificationDAO;

    public long countByUser(Long userId) {
        return certificationDAO.countByIdUser(userId);
    }
}
