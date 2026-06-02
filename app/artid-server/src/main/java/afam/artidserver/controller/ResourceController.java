package afam.artidserver.controller;

import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.dto.ResourceUpsertRequest;
import afam.artidserver.model.entity.User;
import afam.artidserver.service.ResourceService;
import afam.artidserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> findByUser(Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(resourceService.findByUser(user.getId()));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return resourceService.findFileByResourceId(id, user.getId())
                .map(file -> ResponseEntity.ok()
                        .contentType(file.getMimeType() != null
                                ? MediaType.parseMediaType(file.getMimeType())
                                : MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-Disposition",
                                "inline; filename=\"" + (file.getFileName() != null ? file.getFileName() : "file") + "\"")
                        .body(file.getBlob()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @RequestBody ResourceUpsertRequest request,
            Authentication authentication
    ) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(resourceService.create(request, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable Long id,
            @RequestBody ResourceUpsertRequest request,
            Authentication authentication
    ) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return resourceService.update(id, request, user.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        return resourceService.delete(id, user.getId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
