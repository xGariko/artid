package afam.artidserver.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import afam.artidserver.model.dto.TagCreateRequest;
import afam.artidserver.model.dto.TagResponse;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.TagService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagResponse>> findByUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(tagService.findByUser(principal.getId()));

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
        TagResponse nuovoTag = tagService.create(request.title(),
                principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(nuovoTag);

    }
}