package afam.artidserver.controller;

import afam.artidserver.model.dto.AuthResponse;
import afam.artidserver.model.dto.LoginRequest;
import afam.artidserver.model.dto.RegisterRequest;
import afam.artidserver.model.dto.UserResponse;
import afam.artidserver.model.entity.User;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.security.JwtUtil;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // authenticate() carica già l'utente (via CustomUserDetailsService) e verifica la
        // password: riusiamo quel principal invece di rifare una findByMail.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = ((AuthenticatedUser) authentication.getPrincipal()).getUser();

        String token = jwtUtil.generateToken(user.getMail());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getMail(),
                user.getName(),
                user.getSurname()
        ));
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
}
