package afam.artidserver.dao;

import afam.artidserver.model.entity.Resource;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface ResourceDAO extends ListCrudRepository<Resource, Long> {

    long countByIdUserAndDeletedAtIsNull(Long idUser);

    List<Resource> findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(Long idUser);
}
