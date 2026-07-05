package afam.artidserver.service;

import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Step 2 del recupero password (RAD, caso d'uso DIM PASS): dopo la verifica dell'OTP genera una
 * password temporanea casuale, la salva hashata e la invia via email. La verifica dell'OTP e il suo
 * invio sono delegati a {@link OtpService} (caso d'uso GENERA OTP), qui si gestisce solo il reset.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // Testi email definiti verbatim nel RAD (Requisiti Speciali del caso d'uso DIM PASS).
    private static final String SUBJECT = "ARTID - NOTIFICA DI RECUPERO PASSWORD";

    // Alfabeti per la password temporanea: una classe ciascuno così ne garantiamo almeno un
    // carattere per tipo (formato RAD: ≥8 char con maiuscola, minuscola, cifra e speciale).
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%&*?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int LENGTH = 12;

    private final UserService userService;
    private final EmailService emailService;
    // Riusa il BCrypt encoder delle password: la temporanea non viene mai salvata in chiaro.
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom random = new SecureRandom();

    /**
     * Imposta la password del Membro a una nuova generata casualmente (formato RAD), la salva e la
     * invia via email. {@code user} deve essere la riga completa (già caricata dal chiamante) così
     * il {@code save()} di Spring Data JDBC non azzera le colonne non toccate. L'invio è async
     * (fire-and-forget): un fallimento SMTP viene loggato ma non annulla il reset già persistito —
     * l'utente può ripetere la procedura.
     */
    public void resetAndNotify(User user) {
        String tempPassword = generateCompliantPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userService.save(user);
        emailService.sendText(user.getMail(), SUBJECT, buildBody(user.getName(), tempPassword));
    }

    // Un carattere per ogni classe + riempimento fino a LENGTH, poi shuffle Fisher–Yates così le
    // posizioni fisse iniziali non sono prevedibili.
    private String generateCompliantPassword() {
        char[] chars = new char[LENGTH];
        chars[0] = UPPER.charAt(random.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(random.nextInt(LOWER.length()));
        chars[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        chars[3] = SPECIAL.charAt(random.nextInt(SPECIAL.length()));
        for (int i = 4; i < LENGTH; i++) {
            chars[i] = ALL.charAt(random.nextInt(ALL.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private String buildBody(String name, String password) {
        String greetingName = (name == null || name.isBlank()) ? "Membro" : name;
        return "Salve " + greetingName + ", le notifichiamo che è stato richiesto un reset della "
                + "password per il suo account di ArtID. La sua password è " + password + ". Le "
                + "consigliamo di cambiarla una volta che effettuerà il prossimo accesso.";
    }
}
