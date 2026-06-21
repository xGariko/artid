package afam.artidserver.dao;

import afam.artidserver.model.entity.Certification;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface CertificationDAO extends ListCrudRepository<Certification, Long> {

    long countByIdUser(Long idUser);

    List<Certification> findAllByIdUser(Long userId);
}
