package afam.artidserver.controller;

import afam.artidserver.model.dto.PublicArtidDetailResponse;
import afam.artidserver.model.dto.PublicProfileDetailResponse;
import afam.artidserver.model.dto.PublicProfileResponse;
import afam.artidserver.model.entity.User;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Catalogo pubblico per la pagina "Explore": cerca profili pubblici per nome/cognome,
     * professione o titolo di un ArtID pubblico. Query vuota → intero catalogo pubblico.
     */
    @GetMapping("/search")
    public ResponseEntity<List<PublicProfileResponse>> search(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(userService.searchPublicProfiles(query));
    }

    /**
     * Dettaglio pubblico di un profilo (pagina Explore → /explore/[id]). Accessibile senza login.
     * 404 se l'utente non esiste, non è pubblico o è eliminato: la vista espone solo dati e
     * contenuti marcati pubblici.
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<PublicProfileDetailResponse> publicProfile(@PathVariable Long id) {
        return userService.getPublicProfileDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Dettaglio pubblico di un singolo ArtID (Explore → /explore/[id]/artid/[artidId]). Accessibile
     * senza login. 404 se l'ArtID non esiste, non è pubblico o non appartiene a questo utente pubblico.
     */
    @GetMapping("/{userId}/public/artids/{artidId}")
    public ResponseEntity<PublicArtidDetailResponse> publicArtid(@PathVariable Long userId,
                                                                 @PathVariable Long artidId) {
        return userService.getPublicArtidDetail(userId, artidId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        return ResponseEntity.ok(userService.save(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
