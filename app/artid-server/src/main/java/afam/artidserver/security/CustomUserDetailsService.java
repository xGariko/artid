package afam.artidserver.security;

import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userDAO.findByMail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getMail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
