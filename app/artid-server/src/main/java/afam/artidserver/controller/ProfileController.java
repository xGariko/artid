package afam.artidserver.controller;

import afam.artidserver.model.dto.ProfileCompletionResponse;
import afam.artidserver.model.entity.User;
import afam.artidserver.service.ProfileService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;

    @GetMapping("/completion")
    public ResponseEntity<ProfileCompletionResponse> completion(Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new ProfileCompletionResponse(profileService.completionPercentage(user)));
    }
}
