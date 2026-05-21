package afam.artidserver.dao;

import afam.artidserver.model.entity.User;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface UserDAO extends ListCrudRepository<User, Long> {

    Optional<User> findByMail(String mail);
}
