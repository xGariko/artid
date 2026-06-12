package afam.artidserver.controller;

import afam.artidserver.model.dto.ArtidResponse;
import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.ArtidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/artids")
@RequiredArgsConstructor
public class ArtidController {

    private final ArtidService artidService;

    @GetMapping("/count")
    public ResponseEntity<CountResponse> count(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(artidService.countByUser(principal.getId())));
    }

    @GetMapping
    public ResponseEntity<List<ArtidResponse>> findByUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(artidService.findByUser(principal.getId()));
    }
}
