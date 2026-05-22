package afam.artidserver.dao;

import afam.artidserver.model.entity.Artid;
import org.springframework.data.repository.ListCrudRepository;

public interface ArtidDAO extends ListCrudRepository<Artid, Long> {

    long countByIdUserAndDeletedAtIsNull(Long idUser);
}
