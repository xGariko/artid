package afam.artidserver.service;

import afam.artidserver.dao.LoginOtpDAO;
import afam.artidserver.model.entity.LoginOtp;
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
 * Generazione, persistenza e verifica dei codici OTP di accesso (2FA via email).
 * Specifiche da RAD (caso d'uso GENERA OTP): codice numerico a 6 cifre, validità 5 minuti,
 * salvato nel DBMS e inviato all'email del Membro.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    // Testi email definiti verbatim nel RAD (Requisiti Speciali del caso d'uso GENERA OTP).
    private static final String SUBJECT = "ARTID - CODICE DI ACCESSO OTP";

    // Orario di scadenza mostrato all'utente nel fuso italiano (es. "valido fino alle 14:35").
    private static final DateTimeFormatter EXPIRY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Rome"));

    private final LoginOtpDAO loginOtpDAO;
    private final EmailService emailService;
    // Riusa il BCrypt encoder delle password: il codice non viene mai salvato in chiaro.
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    private final SecureRandom random = new SecureRandom();

    @Value("${otp.length:6}")
    private int codeLength;

    @Value("${otp.ttl-seconds:300}")
    private long ttlSeconds;

    // Oltre questo numero di tentativi falliti l'OTP viene invalidato: serve un nuovo invio.
    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    /**
     * Genera un nuovo OTP per l'utente, ne sostituisce l'eventuale precedente e ne avvia l'invio via
     * email. Restituisce l'istante di scadenza appena l'OTP è persistito, SENZA attendere l'SMTP:
     * {@link EmailService#sendHtml} è {@code @Async}, così il chiamante (e quindi l'utente) non resta
     * bloccato sull'I/O di rete dell'invio. L'OTP viene committato PRIMA del dispatch email: la
     * transazione si chiude e restituisce la connessione al pool prima dell'invio (col pooler in
     * transaction mode questo evita di tenere agganciato un backend Supabase), e la verifica funziona
     * anche se la mail arriva con qualche secondo di ritardo. Se l'invio fallisce resta una riga OTP
     * non recapitata: è innocua (la sostituisce il tentativo successivo e scade dopo {@code otp.ttl-seconds}).
     */
    public OffsetDateTime generateAndSend(User user) {
        String code = generateNumericCode();
        // BCrypt è volutamente CPU-intensive: l'hashing sta fuori dalla transazione per tenerla breve.
        String codeHash = passwordEncoder.encode(code);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(ttlSeconds);

        // Delete + insert atomici (UNIQUE su id_user) ma senza l'invio email: la transazione si
        // chiude e restituisce la connessione al pool prima dell'SMTP.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            loginOtpDAO.deleteByIdUser(user.getId());
            LoginOtp otp = new LoginOtp();
            otp.setIdUser(user.getId());
            otp.setCodeHash(codeHash);
            otp.setExpiresAt(expiresAt);
            otp.setAttempts(0);
            otp.setCreatedAt(now);
            loginOtpDAO.save(otp);
        });

        emailService.sendHtml(user.getMail(), SUBJECT, buildBody(user.getName(), code, expiresAt));
        return expiresAt;
    }

    /**
     * Rigenera e rinvia l'OTP ("Riprova") SOLO se esiste già una challenge attiva per l'utente:
     * il rinvio non deve poter essere innescato senza una prima validazione delle credenziali.
     * {@code Optional.empty()} = nessuna challenge in corso. Non transazionale: avvolgerlo terrebbe
     * l'invio email di {@link #generateAndSend} dentro una transazione, agganciando la connessione.
     */
    public Optional<OffsetDateTime> resend(User user) {
        if (loginOtpDAO.findByIdUser(user.getId()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(generateAndSend(user));
    }

    /**
     * Verifica il codice e, se valido, consuma l'OTP (one-time). Invalida l'OTP a scadenza o
     * al superamento dei tentativi. Restituisce {@code true} solo a codice corretto e nei termini.
     */
    @Transactional
    public boolean verify(Long userId, String code) {
        Optional<LoginOtp> found = loginOtpDAO.findByIdUser(userId);
        if (found.isEmpty()) {
            return false;
        }

        LoginOtp otp = found.get();

        if (otp.getExpiresAt().isBefore(OffsetDateTime.now()) || otp.getAttempts() >= maxAttempts) {
            loginOtpDAO.deleteByIdUser(userId);
            return false;
        }

        if (passwordEncoder.matches(code, otp.getCodeHash())) {
            loginOtpDAO.deleteByIdUser(userId);
            return true;
        }

        otp.setAttempts(otp.getAttempts() + 1);
        loginOtpDAO.save(otp);
        return false;
    }

    private String generateNumericCode() {
        int bound = (int) Math.pow(10, codeLength);
        return String.format("%0" + codeLength + "d", random.nextInt(bound));
    }

    private String buildBody(String name, String code, OffsetDateTime expiresAt) {
        return OtpEmailTemplate.render(
                name,
                code,
                EXPIRY_TIME_FORMAT.format(expiresAt),
                "Il tuo codice di accesso",
                "usa questo codice per accedere al tuo account ArtID.");
    }
}
