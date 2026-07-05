package afam.artidserver.service;

import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Cambio password del Membro autenticato (RAD, caso d'uso MODIFICA PASSWORD): la nuova password
 * scelta dall'utente viene applicata solo dopo la verifica di un OTP (riuso del caso d'uso GENERA
 * OTP via {@link OtpService}). Serve anche ai Membri SPID che non hanno mai impostato una password.
 */
@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    // Formato password RAD: ≥8 caratteri con almeno una maiuscola, una minuscola, una cifra e un
    // carattere speciale (qualsiasi non alfanumerico).
    private static final Pattern PASSWORD_FORMAT =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final OtpService otpService;
    private final UserService userService;
    // Riusa il BCrypt encoder: la nuova password non viene mai salvata in chiaro.
    private final PasswordEncoder passwordEncoder;

    /** Esito del cambio: distingue OTP non valido da formato non valido per la risposta HTTP. */
    public enum Result { OK, INVALID_OTP, INVALID_FORMAT }

    /** Step 1: invia l'OTP all'email del Membro (riuso GENERA OTP). */
    public void requestOtp(User user) {
        otpService.generateAndSend(user);
    }

    /**
     * Step 2: valida il formato della nuova password, poi verifica l'OTP e, se entrambi ok, aggiorna
     * la password. Il formato si controlla PRIMA dell'OTP: {@link OtpService#verify} consuma il codice
     * (one-time), quindi non va speso se la password sarebbe comunque rifiutata. La riga utente viene
     * ricaricata completa via {@link UserService#findById}: il principal del filtro JWT non include
     * tutte le colonne (es. propic_path) e un {@code save()} su di esso le azzererebbe.
     */
    public Result changePassword(User user, String code, String newPassword) {
        if (newPassword == null || !PASSWORD_FORMAT.matcher(newPassword).matches()) {
            return Result.INVALID_FORMAT;
        }
        if (!otpService.verify(user.getId(), code)) {
            return Result.INVALID_OTP;
        }
        User full = userService.findById(user.getId()).orElseThrow();
        full.setPasswordHash(passwordEncoder.encode(newPassword));
        // Da qui in poi l'account ha una password reale: anche un Membro nato da SPID potrà eliminarlo
        // con la password invece che con le credenziali SPID.
        full.setPasswordSet(true);
        userService.save(full);
        return Result.OK;
    }
}
