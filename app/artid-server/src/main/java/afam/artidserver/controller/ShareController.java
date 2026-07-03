package afam.artidserver.controller;

import afam.artidserver.model.dto.*;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @GetMapping("/count")
    public ResponseEntity<CountResponse> count(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(shareService.countByUser(principal.getId())));
    }

    @GetMapping("/internal/count") //Non so se serve ma ho messo per coerenza
    public ResponseEntity<CountResponse> internalCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(shareService.countInternalByUser(principal.getId())));
    }

    @GetMapping("/internal")
    public ResponseEntity<List<InternalShareArtIDResponse>> getInternalByAuthor(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(shareService.getInternalFromUser(principal.getId()));
    }

    @GetMapping("/external/count") //Non so se serve ma ho messo per coerenza
    public ResponseEntity<CountResponse> externalCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(shareService.countExternalByUser(principal.getId())));
    }

    @GetMapping("/external")
    public ResponseEntity<List<ExternalShareArtIDResponse>> getExternalByAuthor(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(shareService.getExternalByUser(principal.getId()));
    }
}
