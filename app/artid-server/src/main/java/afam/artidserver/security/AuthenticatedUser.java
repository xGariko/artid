package afam.artidserver.security;

import afam.artidserver.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal autenticato: incapsula lo {@link User} caricato UNA sola volta dal
 * {@link CustomUserDetailsService} dentro il filtro JWT, a inizio richiesta.
 *
 * <p>Prima ogni controller rifaceva una {@code findByMail} per ri-risolvere lo stesso
 * utente: due query identiche al DB per ogni richiesta autenticata (filtro + controller).
 * Ora i controller leggono id/utente da qui via {@code @AuthenticationPrincipal}, quindi
 * la query utente avviene una volta sola e ben visibile, nel filtro.</p>
 */
public class AuthenticatedUser implements UserDetails {

    private final User user;

    public AuthenticatedUser(User user) {
        this.user = user;
    }

    /** Utente completo già caricato dal DB: riusabile nella richiesta senza nuove query. */
    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getMail();
    }
}
