package afam.artidserver.controller;

import afam.artidserver.model.dto.ProfileCompletionResponse;
import afam.artidserver.model.dto.ProfileResponse;
import afam.artidserver.model.dto.ProfileUpdateRequest;
import afam.artidserver.model.entity.User;
import afam.artidserver.service.ProfileService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ProfileResponse> profile(Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(ProfileResponse.from(user));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(Authentication authentication,
                                                         @RequestBody ProfileUpdateRequest request) {
        // L'utente da aggiornare è SEMPRE quello del token, mai un id dal client.
        User user = userService.findByMail(authentication.getName()).orElseThrow();

        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setBirthdate(request.getBirthdate());
        user.setBirthplace(request.getBirthplace());
        user.setAddress(request.getAddress());
        user.setBiography(request.getBiography());
        user.setLinkedinId(request.getLinkedinId());
        user.setProfession(request.getProfession());
        user.setPhone(request.getPhone());
        user.setPropic(request.getPropic());
        user.setInternalShareEnabled(request.isInternalShareEnabled());
        if (request.getIsPublic() != null) {
            user.setIsPublic(request.getIsPublic());
        }
        // mail e passwordHash NON sono toccati: l'identità/credenziali restano invariate.

        User saved = userService.save(user);
        return ResponseEntity.ok(ProfileResponse.from(saved));
    }

    @GetMapping("/completion")
    public ResponseEntity<ProfileCompletionResponse> completion(Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new ProfileCompletionResponse(profileService.completionPercentage(user)));
    }
}
