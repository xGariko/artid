package afam.artidserver.controller;

import afam.artidserver.model.dto.*;
import afam.artidserver.model.entity.User;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.security.JwtUtil;
import afam.artidserver.service.OtpService;
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
}
