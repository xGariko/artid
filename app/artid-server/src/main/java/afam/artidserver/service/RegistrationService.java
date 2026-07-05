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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    private final SecureRandom random = new SecureRandom();

    @Value("${otp.length:6}")
    private int codeLength;

    @Value("${otp.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    /**
     * Avvia la registrazione: salva i dati del form come PENDING, genera l'OTP e ne avvia l'invio.
     * Sostituisce un eventuale pending precedente per la stessa email. Restituisce l'istante di
     * scadenza appena il pending è persistito, SENZA attendere l'SMTP: {@link EmailService#sendText}
     * è {@code @Async}, così l'utente passa subito alla schermata di verifica. Il pending viene
     * committato PRIMA del dispatch email: la transazione si chiude e restituisce la connessione al
     * pool prima dell'invio (col pooler in transaction mode questo evita di tenere agganciato un
     * backend Supabase). Se l'invio fallisce resta un pending non recapitato: è innocuo (lo sostituisce
     * il tentativo successivo e scade dopo {@code otp.ttl-seconds}).
     */
    public OffsetDateTime startChallenge(RegisterRequest request) {
        String code = generateNumericCode();
        // BCrypt è volutamente CPU-intensive: i due hash stanno fuori dalla transazione per tenerla breve.
        String codeHash = passwordEncoder.encode(code);
        // La password viene salvata GIÀ hashata: non resta mai in chiaro, nemmeno nel pending.
        String passwordHash = passwordEncoder.encode(request.getPassword());
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(ttlSeconds);

        // Delete + insert atomici ma senza l'invio email: la transazione si chiude e restituisce
        // la connessione al pool prima dell'SMTP.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            registrationOtpDAO.deleteByEmail(request.getEmail());
            RegistrationOtp pending = new RegistrationOtp();
            pending.setEmail(request.getEmail());
            pending.setName(request.getName());
            pending.setSurname(request.getSurname());
            pending.setPasswordHash(passwordHash);
            pending.setBirthdate(request.getBirthdate());
            pending.setBirthplace(request.getBirthplace());
            pending.setCodeHash(codeHash);
            pending.setExpiresAt(expiresAt);
            pending.setAttempts(0);
            pending.setCreatedAt(now);
            registrationOtpDAO.save(pending);
        });

        emailService.sendHtml(request.getEmail(), SUBJECT, buildBody(request.getName(), code, expiresAt));
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
        // Registrazione classica: la password è quella scelta dall'utente → eliminabile con password.
        user.setPasswordSet(true);
        user.setBirthdate(pending.getBirthdate());
        user.setBirthplace(pending.getBirthplace());
        user.setIsPublic(false);
        // internal_share_enabled è NOT NULL sul DB: senza default esplicito l'INSERT fallirebbe.
        user.setInternalShareEnabled(false);
        return Optional.of(userDAO.save(user));
    }

    /**
     * Rigenera e rinvia l'OTP di registrazione ("Riprova") SOLO se esiste un pending per quell'email.
     * {@code Optional.empty()} = nessuna registrazione in corso (il chiamante non rivela nulla). Non
     * transazionale: l'unico {@code save} è già atomico di per sé e l'invio email avviene dopo, senza
     * tenere agganciata una connessione del pool durante l'SMTP.
     */
    public Optional<OffsetDateTime> resend(String email) {
        Optional<RegistrationOtp> found = registrationOtpDAO.findByEmail(email);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RegistrationOtp pending = found.get();
        String code = generateNumericCode();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(ttlSeconds);
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(expiresAt);
        pending.setAttempts(0);
        registrationOtpDAO.save(pending);

        emailService.sendHtml(email, SUBJECT, buildBody(pending.getName(), code, expiresAt));
        return Optional.of(expiresAt);
    }

    private String generateNumericCode() {
        int bound = (int) Math.pow(10, codeLength);
        return String.format("%0" + codeLength + "d", random.nextInt(bound));
    }

    // Corpo email identico a quello del login: stesso template HTML (OtpEmailTemplate) e stessi
    // heading/intro usati da OtpService.
    private String buildBody(String name, String code, OffsetDateTime expiresAt) {
        return OtpEmailTemplate.render(
                name,
                code,
                EXPIRY_TIME_FORMAT.format(expiresAt),
                "Il tuo codice di accesso",
                "usa questo codice per accedere al tuo account ArtID.");
    }
}
