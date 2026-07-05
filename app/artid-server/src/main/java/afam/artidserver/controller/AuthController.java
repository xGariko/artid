package afam.artidserver.controller;

import afam.artidserver.model.dto.*;
import afam.artidserver.model.entity.User;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.security.JwtUtil;
import afam.artidserver.service.MockSpidIdentityProvider;
import afam.artidserver.service.OtpService;
import afam.artidserver.service.PasswordResetService;
import afam.artidserver.service.RegistrationService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import afam.artidserver.model.dto.VerifyPasswordRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final MockSpidIdentityProvider identityProvider;

    /**
     * Step 1 del login: valida le credenziali e, se corrette, genera l'OTP e ne avvia l'invio
     * via email (asincrono: vedi {@link OtpService#generateAndSend}). NON rilascia il token: la
     * sessione si ottiene solo dopo {@link #verifyOtp}. La risposta torna appena l'OTP è persistito,
     * senza attendere l'SMTP, così il client passa subito alla schermata di verifica.
     */
    @PostMapping("/login")
    public ResponseEntity<OtpChallengeResponse> login(@RequestBody LoginRequest request) {
        User user;
        try {
            // authenticate() carica già l'utente (via CustomUserDetailsService) e verifica la
            // password: riusiamo quel principal invece di rifare una findByMail.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            user = ((AuthenticatedUser) authentication.getPrincipal()).getUser();
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OffsetDateTime expiresAt = otpService.generateAndSend(user);
        return ResponseEntity.ok(new OtpChallengeResponse(true, user.getMail(), expiresAt));
    }

    /**
     * Validazione delle sole credenziali, SENZA generare né inviare l'OTP. Alimenta lo step di
     * conferma (RAD) mostrato prima del dispatch del codice: l'utente arriva all'"Ok" solo con
     * credenziali corrette. 401 se errate, 200 se valide. L'OTP parte poi da {@link #login}.
     */
    @PostMapping("/login/validate")
    public ResponseEntity<Void> validateLogin(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Step 2 del login: verifica l'OTP e, se valido, rilascia il token JWT.
     * Risposta indistinta (401) per email inesistente o codice errato/scaduto.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        User user = userService.findByMail(request.getEmail()).orElse(null);
        if (user == null || !otpService.verify(user.getId(), request.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtUtil.generateToken(user.getMail());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getMail(),
                user.getName(),
                user.getSurname()
        ));
    }

    /**
     * Rigenera e rinvia l'OTP ("Riprova"). Sempre 200 per non rivelare se l'email esiste o se
     * c'è una challenge attiva: il rinvio avviene solo se un OTP era già stato emesso.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@RequestBody ResendOtpRequest request) {
        userService.findByMail(request.getEmail()).ifPresent(otpService::resend);
        return ResponseEntity.ok().build();
    }

    // --- Recupero password (RAD, caso d'uso DIM PASS): riusa il pattern OTP a due step del login. ---

    /**
     * Step 1 del recupero password: se esiste un Membro con quell'email, genera e invia l'OTP
     * (riuso di GENERA OTP) senza resettare ancora nulla. 404 se nessun account corrisponde: come da
     * RAD il client mostra "Non esiste un account con questa email" (scelta esplicita, diversa
     * dall'anti-enumeration di login/registrazione). La risposta torna appena l'OTP è persistito,
     * senza attendere l'SMTP.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<OtpChallengeResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userService.findByMail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        OffsetDateTime expiresAt = otpService.generateAndSend(user);
        return ResponseEntity.ok(new OtpChallengeResponse(true, user.getMail(), expiresAt));
    }

    /**
     * Step 2 del recupero password: verifica l'OTP e, se valido, imposta una password temporanea
     * casuale e la invia via email. NON rilascia una sessione: il RAD riporta al LOGIN. Risposta
     * indistinta (401) per email inesistente o codice errato/scaduto. Il "Riprova" riusa
     * {@link #resendOtp}: l'OTP è una normale challenge sull'account esistente.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody VerifyOtpRequest request) {
        User user = userService.findByMail(request.getEmail()).orElse(null);
        if (user == null || !otpService.verify(user.getId(), request.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        passwordResetService.resetAndNotify(user);
        return ResponseEntity.ok().build();
    }

    /**
     * Step 1 della registrazione: valida che l'email sia libera e avvia la verifica via OTP
     * (email inviata in modo asincrono: vedi {@link RegistrationService#startChallenge}). NON crea
     * l'utente: l'account nasce solo dopo {@link #verifyRegistration}. 409 se l'email è già
     * registrata; la risposta torna appena il pending è persistito, senza attendere l'SMTP.
     */
    @PostMapping("/register")
    public ResponseEntity<OtpChallengeResponse> register(@RequestBody RegisterRequest request) {
        if (userService.findByMail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        OffsetDateTime expiresAt = registrationService.startChallenge(request);
        return ResponseEntity.ok(new OtpChallengeResponse(true, request.getEmail(), expiresAt));
    }

    /**
     * Validazione della sola disponibilità dell'email, SENZA creare la registrazione pending né
     * inviare l'OTP. Alimenta lo step di conferma (RAD) prima del dispatch del codice. 409 se
     * l'email è già registrata, 200 se libera. La challenge parte poi da {@link #register}.
     */
    @PostMapping("/register/validate")
    public ResponseEntity<Void> validateRegistration(@RequestBody RegisterRequest request) {
        if (userService.findByMail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Step 2 della registrazione: verifica l'OTP e, se valido, CREA l'utente e rilascia il token.
     * Risposta indistinta (401) per codice errato/scaduto o nessuna registrazione in corso.
     */
    @PostMapping("/verify-registration")
    public ResponseEntity<AuthResponse> verifyRegistration(@RequestBody VerifyOtpRequest request) {
        var created = registrationService.verifyAndCreate(request.getEmail(), request.getCode());
        if (created.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = created.get();
        String token = jwtUtil.generateToken(user.getMail());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getMail(),
                user.getName(),
                user.getSurname()
        ));
    }

    /**
     * Rigenera e rinvia l'OTP di registrazione ("Riprova"). Sempre 200 per non rivelare se esiste
     * una registrazione in corso per quell'email: il rinvio avviene solo se un pending è presente.
     */
    @PostMapping("/resend-registration-otp")
    public ResponseEntity<Void> resendRegistration(@RequestBody ResendOtpRequest request) {
        registrationService.resend(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        // L'utente è già stato caricato dal filtro JWT: nessuna query qui.
        User user = principal.getUser();
        return ResponseEntity.ok(new UserResponse(
                user.getId(),
                user.getMail(),
                user.getName(),
                user.getSurname()
        ));
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody VerifyPasswordRequest request) {

        User user = principal.getUser();
        boolean isCorrect = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        Map<String, Boolean> response = new HashMap<>();
        response.put("passwordCorretta", isCorrect);

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica le credenziali SPID del Membro autenticato (gemello di {@link #verifyPassword} per gli
     * account nati da SPID, che non hanno una password reale). Serve alla modale "Chiudi account":
     * quando {@code passwordSet} è false il client chiede codice fiscale + password del provider invece
     * della password. Esito {@code true} solo se le credenziali autenticano presso il provider mock E
     * l'identità autenticata è QUELLA collegata all'account ({@code spid_code} del profilo == CF
     * inserito): impedisce di eliminare l'account con le credenziali SPID di un terzo. Sempre 200 con
     * l'esito nel body {@code {"spidCorretta": ...}}, coerente con verify-password.
     */
    @PostMapping("/verify-spid")
    public ResponseEntity<Map<String, Boolean>> verifySpid(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody SpidLoginRequest request) {

        User user = principal.getUser();
        boolean isCorrect = user.getSpidCode() != null
                && identityProvider.authenticate(request.getUsername(), request.getPassword())
                        .map(identity -> identity.username().equalsIgnoreCase(user.getSpidCode()))
                        .orElse(false);

        Map<String, Boolean> response = new HashMap<>();
        response.put("spidCorretta", isCorrect);

        return ResponseEntity.ok(response);
    }
}
