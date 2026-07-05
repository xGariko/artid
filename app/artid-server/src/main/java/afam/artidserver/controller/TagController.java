package afam.artidserver.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

        try {
            TagResponse nuovoTag = tagService.create(request.title(), principal.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(nuovoTag);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

    }

    // DELETE /api/tags/{id} -> Elimina definitivamente un tag della libreria dell'utente loggato.
    // 204 se eliminato, 404 se il tag non esiste o non è di questo utente. Le associazioni artid_tag
    // sono rimosse in cascata dal DB.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return tagService.delete(id, principal.getId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}