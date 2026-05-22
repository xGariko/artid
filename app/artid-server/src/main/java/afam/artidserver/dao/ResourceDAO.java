package afam.artidserver.dao;

import afam.artidserver.model.entity.Resource;
import org.springframework.data.repository.ListCrudRepository;

public interface ResourceDAO extends ListCrudRepository<Resource, Long> {

    long countByIdUserAndDeletedAtIsNull(Long idUser);
}
