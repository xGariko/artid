package afam.artidserver.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import afam.artidserver.model.dto.ArtidResponse;
import afam.artidserver.model.dto.TagCreateRequest;
import afam.artidserver.model.dto.TagResponse;
import afam.artidserver.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    // private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagResponse>> findByUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        // return ResponseEntity.ok(tagService.findByUser(principal.getId()));

        List<TagResponse> tags = List.of(
                new TagResponse(1L, "test", "4f9a26"),
                new TagResponse(2L, "ciao", "b339a1"),
                new TagResponse(3L, "prova", "1a7cd3"),
                new TagResponse(4L, "test", "e6b800"));
        return ResponseEntity.ok(tags);

    }

    // POST /api/tags -> Crea un nuovo tag per l'utente loggato
    @PostMapping
    public ResponseEntity<TagResponse> create(@RequestBody TagCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        // Validazione di sicurezza: il titolo non può essere vuoto
        if (request.title() == null || request.title().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Salva il tag legandolo all'ID dell'utente autenticato
        // TagResponse nuovoTag = tagService.create(request.title(), request.color(),
        // principal.getId());

        // TODO rimuovere
        TagResponse nuovoTag = new TagResponse(
                1L,
                request.title() != null ? request.title() : "no title",
                request.color() != null ? request.color() : "EFBF04");
        return ResponseEntity.ok(nuovoTag);
    }
}