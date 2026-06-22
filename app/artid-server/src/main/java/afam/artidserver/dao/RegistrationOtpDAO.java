package afam.artidserver.dao;

import afam.artidserver.model.entity.RegistrationOtp;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface RegistrationOtpDAO extends ListCrudRepository<RegistrationOtp, Long> {

    Optional<RegistrationOtp> findByEmail(String email);

    void deleteByEmail(String email);
}
