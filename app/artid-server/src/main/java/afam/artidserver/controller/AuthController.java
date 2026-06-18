package afam.artidserver.controller;

import afam.artidserver.model.dto.*;
import afam.artidserver.model.entity.User;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.security.JwtUtil;
import afam.artidserver.service.OtpService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
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

    /**
     * Step 1 del login: valida le credenziali e, se corrette, invia un OTP via email.
     * NON rilascia il token: la sessione si ottiene solo dopo {@link #verifyOtp}.
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

        try {
            OffsetDateTime expiresAt = otpService.generateAndSend(user);
            return ResponseEntity.ok(new OtpChallengeResponse(true, user.getMail(), expiresAt));
        } catch (MailException e) {
            // Credenziali ok ma SMTP irraggiungibile: distinguibile dal 401 lato client.
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userService.findByMail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setMail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setBirthdate(request.getBirthdate());
        user.setBirthplace(request.getBirthplace());
        user.setIsPublic(false);
        // internal_share_enabled è NOT NULL sul DB: senza default esplicito l'INSERT fallisce.
        user.setInternalShareEnabled(false);

        User saved = userService.save(user);
        String token = jwtUtil.generateToken(saved.getMail());
        return ResponseEntity.ok(new AuthResponse(
                token,
                saved.getId(),
                saved.getMail(),
                saved.getName(),
                saved.getSurname()
        ));
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
