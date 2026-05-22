package afam.artidserver.controller;

import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.model.entity.User;
import afam.artidserver.service.ResourceService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final UserService userService;

    @GetMapping("/count")
    public ResponseEntity<CountResponse> count(Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new CountResponse(resourceService.countByUser(user.getId())));
    }
}
