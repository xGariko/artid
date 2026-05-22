package afam.artidserver.dao;

import afam.artidserver.model.entity.Certification;
import org.springframework.data.repository.ListCrudRepository;

public interface CertificationDAO extends ListCrudRepository<Certification, Long> {

    long countByIdUser(Long idUser);
}
