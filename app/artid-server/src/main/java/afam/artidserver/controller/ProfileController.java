package afam.artidserver.controller;

import afam.artidserver.model.dto.AvatarResponse;
import afam.artidserver.model.dto.ProfileCompletionResponse;
import afam.artidserver.model.dto.ProfileResponse;
import afam.artidserver.model.dto.ProfileUpdateRequest;
import afam.artidserver.model.entity.User;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.AvatarService;
import afam.artidserver.service.ProfileService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;
    private final AvatarService avatarService;

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

    @GetMapping("/completion")
    public ResponseEntity<ProfileCompletionResponse> completion(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new ProfileCompletionResponse(profileService.completionPercentage(principal.getUser())));
    }

    // --- Foto profilo (bucket privato "propics", servita via presigned URL) ---

    @GetMapping("/avatar")
    public ResponseEntity<AvatarResponse> avatar(@AuthenticationPrincipal AuthenticatedUser principal) {
        // Query dedicata e minimale (sola object key) + firma: la navbar la usa su ogni pagina.
        return ResponseEntity.ok(new AvatarResponse(avatarService.presignedUrlFor(principal.getId()).orElse(null)));
    }

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
