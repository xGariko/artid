package afam.artidserver.dao;

import afam.artidserver.model.entity.LoginOtp;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface LoginOtpDAO extends ListCrudRepository<LoginOtp, Long> {

    Optional<LoginOtp> findByIdUser(Long idUser);

    void deleteByIdUser(Long idUser);
}
