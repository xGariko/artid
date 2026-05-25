package afam.artidserver.dao;

import afam.artidserver.model.entity.File;
import org.springframework.data.repository.ListCrudRepository;

public interface FileDAO extends ListCrudRepository<File, Long> {
}
