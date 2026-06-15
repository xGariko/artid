package afam.artidserver.controller;

import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping("/count")
    public ResponseEntity<CountResponse> count(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(certificationService.countByUser(principal.getId())));
    }
}
