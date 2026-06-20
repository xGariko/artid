package afam.artidserver.controller;

import afam.artidserver.model.dto.AuthResponse;
import afam.artidserver.model.dto.SpidAuthResponse;
import afam.artidserver.model.dto.SpidLoginRequest;
import afam.artidserver.model.dto.SpidVerifyOtpRequest;
import afam.artidserver.service.SpidAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Autenticazione con Identità digitale (SPID) — RAD, caso d'uso AUT_MEM_ID. Il provider è mockato:
 * vedi {@link afam.artidserver.service.MockSpidIdentityProvider}. Tenuto separato da
 * {@link AuthController} per non appesantirlo ulteriormente.
 */
@RestController
@RequestMapping("/api/auth/spid")
@RequiredArgsConstructor
public class SpidController {

    private final SpidAuthService spidAuthService;

    /**
     * Step 1: autentica le credenziali presso il provider mock. Esiti: 200 {@code AUTHENTICATED}
     * (token rilasciato) oppure 200 {@code OTP_REQUIRED} (email già registrata → OTP inviato);
     * 401 se l'autenticazione presso il provider fallisce; 502 se l'invio OTP fallisce.
     */
    @PostMapping
    public ResponseEntity<SpidAuthResponse> login(@RequestBody SpidLoginRequest request) {
        Optional<SpidAuthResponse> result;
        try {
            result = spidAuthService.authenticate(request);
        } catch (MailException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        return result
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Step 2 (solo ramo collisione email): verifica l'OTP e, se valido, collega lo spidCode
     * all'account esistente e rilascia il token. 401 indistinto per codice errato/scaduto o dati
     * non coerenti.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody SpidVerifyOtpRequest request) {
        return spidAuthService.verifyOtp(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
