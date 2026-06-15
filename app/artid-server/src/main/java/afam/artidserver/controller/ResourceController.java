package afam.artidserver.controller;

import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.dto.ResourceUpsertRequest;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/count")
    public ResponseEntity<CountResponse> count(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(new CountResponse(resourceService.countByUser(principal.getId())));
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> findByUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(resourceService.findByUser(principal.getId()));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return resourceService.findDownloadable(id, principal.getId())
                .map(file -> ResponseEntity.ok()
                        .contentType(file.mimeType() != null
                                ? MediaType.parseMediaType(file.mimeType())
                                : MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-Disposition",
                                "inline; filename=\"" + (file.fileName() != null ? file.fileName() : "file") + "\"")
                        .body(file.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> create(
            @ModelAttribute ResourceUpsertRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(resourceService.create(request, file, principal.getId()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> update(
            @PathVariable Long id,
            @ModelAttribute ResourceUpsertRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return resourceService.update(id, request, file, principal.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return resourceService.delete(id, principal.getId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
