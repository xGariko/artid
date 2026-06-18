package afam.artidserver.service;

import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.dto.AuthResponse;
import afam.artidserver.model.dto.SpidAuthResponse;
import afam.artidserver.model.dto.SpidLoginRequest;
import afam.artidserver.model.dto.SpidVerifyOtpRequest;
import afam.artidserver.model.entity.User;
import afam.artidserver.model.mock.MockSpidIdentity;
import afam.artidserver.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrazione dell'autenticazione SPID (RAD, caso d'uso AUT_MEM_ID). Il provider è mockato
 * ({@link MockSpidIdentityProvider}); tutto ciò che sta a valle — provisioning utente, OTP, JWT —
 * usa l'architettura reale. Flusso, dopo l'autenticazione presso il provider:
 * <ol>
 *   <li>match per {@code spidCode} → login diretto;</li>
 *   <li>email libera → crea l'utente con lo spidCode e autentica;</li>
 *   <li>email già di un account senza spidCode → invia OTP; sessione solo dopo {@link #verifyOtp}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SpidAuthService {

    private final MockSpidIdentityProvider identityProvider;
    private final UserDAO userDAO;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Step 1: autentica presso il provider mock e applica la logica di provisioning RAD.
     * {@code Optional.empty()} = autenticazione presso il provider fallita (→ 401). Può propagare
     * {@code MailException} nel ramo OTP (→ il controller risponde 502).
     */
    @Transactional
    public Optional<SpidAuthResponse> authenticate(SpidLoginRequest request) {
        Optional<MockSpidIdentity> authenticated =
                identityProvider.authenticate(request.getUsername(), request.getPassword());
        if (authenticated.isEmpty()) {
            return Optional.empty();
        }
        MockSpidIdentity identity = authenticated.get();

        // 1) L'identità è già collegata a un utente: login diretto.
        Optional<User> bySpid = userDAO.findBySpidCode(identity.username());
        if (bySpid.isPresent()) {
            return Optional.of(toAuthenticated(bySpid.get()));
        }

        // 2) Email libera: crea l'utente con lo spidCode e autentica.
        Optional<User> byMail = userDAO.findByMail(identity.email());
        if (byMail.isEmpty()) {
            return Optional.of(toAuthenticated(provisionUser(identity)));
        }

        // 3) Email già registrata senza spidCode: prima dell'aggancio serve la verifica OTP (RAD).
        OffsetDateTime expiresAt = otpService.generateAndSend(byMail.get());
        return Optional.of(SpidAuthResponse.otpRequired(byMail.get().getMail(), expiresAt));
    }

    /**
     * Step 2 (solo ramo collisione email): verifica l'OTP e, se valido, sovrascrive i dati anagrafici
     * dell'account esistente con quelli del provider e ne collega lo spidCode, poi rilascia il token.
     * {@code Optional.empty()} = identità/account inesistenti, mismatch o OTP errato/scaduto (→ 401).
     */
    @Transactional
    public Optional<AuthResponse> verifyOtp(SpidVerifyOtpRequest request) {
        Optional<MockSpidIdentity> identityOpt = identityProvider.findByUsername(request.getUsername());
        Optional<User> userOpt = userDAO.findByMail(request.getEmail());
        if (identityOpt.isEmpty() || userOpt.isEmpty()) {
            return Optional.empty();
        }
        MockSpidIdentity identity = identityOpt.get();
        User user = userOpt.get();

        // L'identità mock deve appartenere all'account per cui è stato emesso l'OTP.
        if (!identity.email().equalsIgnoreCase(user.getMail())) {
            return Optional.empty();
        }
        if (!otpService.verify(user.getId(), request.getCode())) {
            return Optional.empty();
        }

        user.setName(identity.name());
        user.setSurname(identity.surname());
        user.setBirthdate(identity.birthdate());
        user.setBirthplace(identity.birthplace());
        user.setSpidCode(identity.username());
        userDAO.save(user);

        String token = jwtUtil.generateToken(user.getMail());
        return Optional.of(new AuthResponse(
                token, user.getId(), user.getMail(), user.getName(), user.getSurname()));
    }

    // Crea un nuovo utente dai dati del provider. password_hash è NOT NULL ma l'utente SPID non ha
    // (ancora) una password: gli si assegna un hash inutilizzabile; potrà impostarla con "Modifica
    // Password" (nota del RAD). isPublic/internalShareEnabled come nella registrazione classica.
    private User provisionUser(MockSpidIdentity identity) {
        User user = new User();
        user.setName(identity.name());
        user.setSurname(identity.surname());
        user.setMail(identity.email());
        user.setSpidCode(identity.username());
        user.setBirthdate(identity.birthdate());
        user.setBirthplace(identity.birthplace());
        user.setIsPublic(false);
        user.setInternalShareEnabled(false);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        return userDAO.save(user);
    }

    private SpidAuthResponse toAuthenticated(User user) {
        String token = jwtUtil.generateToken(user.getMail());
        return SpidAuthResponse.authenticated(
                token, user.getId(), user.getMail(), user.getName(), user.getSurname());
    }
}
