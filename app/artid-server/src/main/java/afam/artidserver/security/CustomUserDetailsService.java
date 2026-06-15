package afam.artidserver.security;

import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Query leggera (senza l'immagine propic): è l'hot path eseguito a ogni richiesta.
        User user = userDAO.findByMailForAuth(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));

        // Restituiamo l'utente come principal: i controller lo riusano nella stessa
        // richiesta senza una seconda findByMail.
        return new AuthenticatedUser(user);
    }
}
