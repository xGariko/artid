package afam.artidserver.controller;

import afam.artidserver.model.dto.AvatarResponse;
import afam.artidserver.model.dto.ChangePasswordRequest;
import afam.artidserver.model.dto.ProfileCompletionResponse;
import afam.artidserver.model.dto.ProfileResponse;
import afam.artidserver.model.dto.ProfileUpdateRequest;
import afam.artidserver.model.dto.SpidLoginRequest;
import afam.artidserver.model.entity.User;
import afam.artidserver.model.mock.MockSpidIdentity;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.AvatarService;
import afam.artidserver.service.MockSpidIdentityProvider;
import afam.artidserver.service.PasswordChangeService;
import afam.artidserver.service.ProfileService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;
    private final AvatarService avatarService;
    private final MockSpidIdentityProvider identityProvider;
    private final PasswordChangeService passwordChangeService;

    @GetMapping
    public ResponseEntity<ProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUser principal) {
        // Carichiamo la riga completa (include propic_path) con una query esplicita; il presigned
        // URL dell'avatar si firma dalla key che abbiamo già, senza ulteriori query al DB.
        User user = userService.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(ProfileResponse.from(user, avatarService.presignKey(user.getPropicPath())));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @RequestBody ProfileUpdateRequest request) {
        // L'utente da aggiornare è SEMPRE quello del token (mai un id dal client). Carichiamo
        // la riga completa così il save() non azzera campi non toccati (es. propic_path: la foto
        // si gestisce dagli endpoint /avatar, non da questo PUT).
        User user = userService.findById(principal.getId()).orElseThrow();

        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setBirthdate(request.getBirthdate());
        user.setBirthplace(request.getBirthplace());
        user.setAddress(request.getAddress());
        user.setBiography(request.getBiography());
        user.setLinkedinId(request.getLinkedinId());
        user.setFacebookId(request.getFacebookId());
        user.setInstagramId(request.getInstagramId());
        user.setProfession(request.getProfession());
        user.setPhone(request.getPhone());
        user.setBusinessEmail(request.getBusinessEmail());
        user.setInternalShareEnabled(request.isInternalShareEnabled());
        if (request.getIsPublic() != null) {
            user.setIsPublic(request.getIsPublic());
        }
        // mail, passwordHash e propic_path NON sono toccati qui.

        User saved = userService.save(user);
        return ResponseEntity.ok(ProfileResponse.from(saved, avatarService.presignKey(saved.getPropicPath())));
    }

    /**
     * Collega SPID al profilo del Membro corrente (RAD, caso d'uso COL_SPID). Il provider è mockato:
     * autentica le credenziali, poi SOVRASCRIVE l'anagrafica con i dati restituiti dal provider e
     * associa lo spidCode (l'account diventa "verificato"). L'utente è SEMPRE quello del token.
     * 401 se l'autenticazione presso il provider fallisce.
     */
    @PostMapping("/spid")
    public ResponseEntity<ProfileResponse> linkSpid(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @RequestBody SpidLoginRequest request) {
        Optional<MockSpidIdentity> identity =
                identityProvider.authenticate(request.getUsername(), request.getPassword());
        if (identity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MockSpidIdentity spid = identity.get();

        // Un'identità SPID può essere collegata a UN SOLO account ArtID. Se quel codice è già
        // associato a un altro utente, rifiutiamo con 409 invece di creare un duplicato: due righe
        // con lo stesso spid_code farebbero poi fallire il login SPID (findBySpidCode si aspetta un
        // unico risultato). Il re-link sullo stesso account è ammesso (no-op).
        Optional<User> alreadyLinked = userService.findBySpidCode(spid.username());
        if (alreadyLinked.isPresent() && !alreadyLinked.get().getId().equals(principal.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Carichiamo la riga completa così il save() non azzera i campi non toccati (es. propic_path).
        User user = userService.findById(principal.getId()).orElseThrow();
        // Nota RAD: le informazioni anagrafiche vengono sostituite con quelle del provider.
        user.setName(spid.name());
        user.setSurname(spid.surname());
        user.setBirthdate(spid.birthdate());
        user.setBirthplace(spid.birthplace());
        user.setSpidCode(spid.username());
        User saved = userService.save(user);

        return ResponseEntity.ok(ProfileResponse.from(saved, avatarService.presignKey(saved.getPropicPath())));
    }

    @GetMapping("/completion")
    public ResponseEntity<ProfileCompletionResponse> completion(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new ProfileCompletionResponse(profileService.completionPercentage(principal.getUser())));
    }

    // --- Cambio password (RAD, caso d'uso MODIFICA PASSWORD): la nuova password si applica solo dopo
    // la verifica di un OTP inviato all'email del Membro autenticato. ---

    /** Step 1: invia l'OTP all'email del Membro autenticato (riuso GENERA OTP). */
    @PostMapping("/change-password/request-otp")
    public ResponseEntity<Void> changePasswordRequestOtp(@AuthenticationPrincipal AuthenticatedUser principal) {
        passwordChangeService.requestOtp(principal.getUser());
        return ResponseEntity.ok().build();
    }

    /**
     * Step 2: verifica l'OTP e, se valido, imposta la nuova password. 401 per OTP errato/scaduto,
     * 400 se la nuova password non rispetta il formato richiesto.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestBody ChangePasswordRequest request) {
        return switch (passwordChangeService.changePassword(
                principal.getUser(), request.getCode(), request.getNewPassword())) {
            case OK -> ResponseEntity.ok().build();
            case INVALID_OTP -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            case INVALID_FORMAT -> ResponseEntity.badRequest().build();
        };
    }

    // --- Foto profilo (bucket privato "propics", servita via presigned URL) ---

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarResponse> uploadAvatar(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(new AvatarResponse(avatarService.upload(principal.getId(), file)));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal AuthenticatedUser principal) {
        avatarService.delete(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
