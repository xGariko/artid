package afam.artidserver.controller;

import afam.artidserver.model.dto.ArtidCreateRequest;
import afam.artidserver.model.dto.ArtidResponse;
import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.ArtidService;
import afam.artidserver.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/artids")
@RequiredArgsConstructor
public class ArtidController {

    private final ArtidService artidService;
    private final ResourceService resourceService;

    @GetMapping("/count")
    public ResponseEntity<CountResponse> count(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(artidService.countByUser(principal.getId())));
    }

    @GetMapping
    public ResponseEntity<List<ArtidResponse>> findByUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(artidService.findByUser(principal.getId()));
    }

    // Dettaglio di un ArtID dell'utente loggato. La proprietà è verificata nella query del
    // service (filtro per id_user del principal): id non posseduto/inesistente/eliminato → 404.
    @GetMapping("/{id}")
    public ResponseEntity<ArtidResponse> findById(@PathVariable Long id,
                                                  @AuthenticationPrincipal AuthenticatedUser principal) {
        return artidService.findByIdForUser(id, principal.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crea un ArtID (solo titolo) intestato all'utente loggato: l'id_user arriva dal principal,
    // mai dal client, quindi non è possibile creare ArtID per conto di altri.
    @PostMapping
    public ResponseEntity<ArtidResponse> create(@RequestBody ArtidCreateRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser principal) {
        if (request.title() == null || request.title().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(artidService.create(request.title(), principal.getId()));
    }

    // Materiali collegati a un ArtID. La consistenza di proprietà è verificata nella query del
    // service (sia l'ArtID sia i materiali devono essere dell'utente del JWT): un ArtID non
    // posseduto restituisce lista vuota, mai materiali di altri.
    @GetMapping("/{id}/resources")
    public ResponseEntity<List<ResourceResponse>> findResources(@PathVariable Long id,
                                                                @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(resourceService.findByArtid(id, principal.getId()));
    }
}
