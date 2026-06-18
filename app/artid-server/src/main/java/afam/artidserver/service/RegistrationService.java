package afam.artidserver.service;

import afam.artidserver.dao.RegistrationOtpDAO;
import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.dto.RegisterRequest;
import afam.artidserver.model.entity.RegistrationOtp;
import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Verifica email in fase di REGISTRAZIONE via OTP. A differenza di {@link OtpService} (login, OTP
 * legato a un utente esistente) qui l'utente non esiste ancora: i dati del form vivono in
 * registration_otp finché il codice non è verificato, poi si crea l'utente vero. Stesse regole del
 * login (impostazioni {@code otp.*}): codice numerico a 6 cifre, validità 5 minuti, hash BCrypt.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String SUBJECT = "ARTID - CODICE DI VERIFICA REGISTRAZIONE";

    // Orario di scadenza mostrato nel fuso italiano (es. "valido fino alle 14:35").
    private static final DateTimeFormatter EXPIRY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Rome"));

    private final RegistrationOtpDAO registrationOtpDAO;
    private final UserDAO userDAO;
    private final EmailService emailService;
    // Riusa il BCrypt encoder: né la password né il codice vengono mai salvati in chiaro.
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom random = new SecureRandom();

    @Value("${otp.length:6}")
    private int codeLength;

    @Value("${otp.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    /**
     * Avvia la registrazione: salva i dati del form come PENDING, genera e invia l'OTP. Sostituisce
     * un eventuale pending precedente per la stessa email. Restituisce l'istante di scadenza. Se
     * l'invio email fallisce la transazione viene annullata (nessun pending "fantasma" nel DB).
     */
    @Transactional
    public OffsetDateTime startChallenge(RegisterRequest request) {
        String code = generateNumericCode();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(ttlSeconds);

        registrationOtpDAO.deleteByEmail(request.getEmail());

        RegistrationOtp pending = new RegistrationOtp();
        pending.setEmail(request.getEmail());
        pending.setName(request.getName());
        pending.setSurname(request.getSurname());
        // La password viene salvata GIÀ hashata: non resta mai in chiaro, nemmeno nel pending.
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setBirthdate(request.getBirthdate());
        pending.setBirthplace(request.getBirthplace());
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(expiresAt);
        pending.setAttempts(0);
        pending.setCreatedAt(now);
        registrationOtpDAO.save(pending);

        emailService.sendText(request.getEmail(), SUBJECT, buildBody(request.getName(), code, expiresAt));
        return expiresAt;
    }

    /**
     * Verifica il codice e, se valido, consuma il pending e CREA l'utente. Invalida l'OTP a scadenza
     * o al superamento dei tentativi. {@code Optional.empty()} = codice errato/scaduto o nessuna
     * registrazione in corso.
     */
    @Transactional
    public Optional<User> verifyAndCreate(String email, String code) {
        Optional<RegistrationOtp> found = registrationOtpDAO.findByEmail(email);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RegistrationOtp pending = found.get();

        if (pending.getExpiresAt().isBefore(OffsetDateTime.now()) || pending.getAttempts() >= maxAttempts) {
            registrationOtpDAO.deleteByEmail(email);
            return Optional.empty();
        }

        if (!passwordEncoder.matches(code, pending.getCodeHash())) {
            pending.setAttempts(pending.getAttempts() + 1);
            registrationOtpDAO.save(pending);
            return Optional.empty();
        }

        // Codice corretto: consuma il pending. Se nel frattempo l'email è già diventata un utente
        // (verifica concorrente o registrazione parallela), non creiamo un duplicato.
        registrationOtpDAO.deleteByEmail(email);
        if (userDAO.findByMail(email).isPresent()) {
            return Optional.empty();
        }

        User user = new User();
        user.setName(pending.getName());
        user.setSurname(pending.getSurname());
        user.setMail(email);
        user.setPasswordHash(pending.getPasswordHash()); // già hashata in startChallenge
        user.setBirthdate(pending.getBirthdate());
        user.setBirthplace(pending.getBirthplace());
        user.setIsPublic(false);
        // internal_share_enabled è NOT NULL sul DB: senza default esplicito l'INSERT fallirebbe.
        user.setInternalShareEnabled(false);
        return Optional.of(userDAO.save(user));
    }

    /**
     * Rigenera e rinvia l'OTP di registrazione ("Riprova") SOLO se esiste un pending per quell'email.
     * {@code Optional.empty()} = nessuna registrazione in corso (il chiamante non rivela nulla).
     */
    @Transactional
    public Optional<OffsetDateTime> resend(String email) {
        Optional<RegistrationOtp> found = registrationOtpDAO.findByEmail(email);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RegistrationOtp pending = found.get();
        String code = generateNumericCode();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(ttlSeconds);
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(expiresAt);
        pending.setAttempts(0);
        registrationOtpDAO.save(pending);

        emailService.sendText(email, SUBJECT, buildBody(pending.getName(), code, expiresAt));
        return Optional.of(expiresAt);
    }

    private String generateNumericCode() {
        int bound = (int) Math.pow(10, codeLength);
        return String.format("%0" + codeLength + "d", random.nextInt(bound));
    }

    private String buildBody(String name, String code, OffsetDateTime expiresAt) {
        String greetingName = (name == null || name.isBlank()) ? "Membro" : name;
        return "Salve " + greetingName + ", per completare la registrazione ad ArtID inserisci il "
                + "codice di verifica: " + code + ". Il codice sarà valido fino alle "
                + EXPIRY_TIME_FORMAT.format(expiresAt) + ", non condividerlo con nessuno. "
                + "SE NON SEI STATO TU A RICHIEDERLO IGNORA QUESTA EMAIL.";
    }
}
