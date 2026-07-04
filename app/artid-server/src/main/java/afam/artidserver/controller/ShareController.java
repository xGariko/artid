package afam.artidserver.controller;

import afam.artidserver.model.dto.*;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/internal/count") // Non so se serve ma ho messo per coerenza
    public ResponseEntity<CountResponse> internalCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(shareService.countInternalByUser(principal.getId())));
    }

    @GetMapping("/internal")
    public ResponseEntity<List<InternalShareArtIDResponse>> getInternalByAuthor(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(shareService.getInternalFromUser(principal.getId()));
    }

    @GetMapping("/internal/to-me")
    public ResponseEntity<List<InternalShareArtIDExtendedResponse>> getInternalToMe(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(shareService.getInternalToUser(principal.getId()));
    }

    @PutMapping("/internal/decline")
    public ResponseEntity<Void> declineInternalShare(
            @RequestBody Long idArtid,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        // Il Service lancerà un'eccezione se l'utente loggato non è il reale
        // destinatario
        shareService.declineInternalShare(idArtid, principal.getId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/external/count") // Non so se serve ma ho messo per coerenza
    public ResponseEntity<CountResponse> externalCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(shareService.countExternalByUser(principal.getId())));
    }

    @GetMapping("/external")
    public ResponseEntity<List<ExternalShareArtIDResponse>> getExternalByAuthor(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(shareService.getExternalByUser(principal.getId()));
    }

    // Crea una condivisione esterna per un ArtID dell'utente; restituisce id +
    // token del link pubblico.
    @PostMapping("/external")
    public ResponseEntity<CreateExternalShareResponse> createExternalShare(
            @AuthenticationPrincipal AuthenticatedUser principal, @RequestBody CreateExternalShareRequest request) {
        return ResponseEntity.ok(shareService.createExternalShare(principal.getId(), request.artidId(),
                request.expirationDate(), request.description()));
    }

    @PatchMapping("/disable")
    public ResponseEntity<Void> disableShares(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody List<Long> shareIds) {
        shareService.disableAll(principal.getId(), shareIds);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/enable")
    public ResponseEntity<Void> enableShares(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody List<Long> shareIds) {
        shareService.enableAll(principal.getId(), shareIds);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/external/{id}/expiration")
    public ResponseEntity<Void> extendExpiration(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id, @RequestBody ExtendExpirationRequest request) {
        shareService.extendExpiration(principal.getId(), id, request.expirationDate());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/external/{id}/description")
    public ResponseEntity<Void> updateDescription(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id, @RequestBody ShareDescriptionUpdateRequest request) {
        shareService.updateDescription(principal.getId(), id, request.description());
        return ResponseEntity.noContent().build();
    }

    // Genera il token del link pubblico per una condivisione esterna dell'utente
    // (owner-only).
    @GetMapping("/external/{id}/link")
    public ResponseEntity<ShareLinkResponse> generateLink(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(new ShareLinkResponse(shareService.generateLink(principal.getId(), id)));
    }

    // Apertura del link pubblico (/s/[token]): senza login. Registra la
    // visualizzazione e restituisce
    // l'anteprima dell'ArtID collegato, purché la condivisione sia attiva e non
    // scaduta.
    @GetMapping("/public/{token}")
    public ResponseEntity<PublicArtidDetailResponse> openSharedArtid(@PathVariable String token) {
        return ResponseEntity.ok(shareService.openSharedArtid(token));
    }

    @DeleteMapping("/external")
    public ResponseEntity<Void> deleteExternalShares(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody List<Long> shareIds) {
        shareService.deleteExternalShares(principal.getId(), shareIds);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal")
    public ResponseEntity<Void> deleteShares(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody List<Long> shareIds) {
        shareService.deleteInternalShares(principal.getId(), shareIds);
        return ResponseEntity.noContent().build();
    }

}
