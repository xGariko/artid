package afam.artidserver.controller;

import afam.artidserver.model.dto.CertificationResponse;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping
    public ResponseEntity<List<CertificationResponse>> getAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(certificationService.findAllCertifications(principal.getId()));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return certificationService.findDownloadable(id, principal.getId())
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
    public ResponseEntity<CertificationResponse> create(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("isPublic") boolean isPublic,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser principal) throws IOException {

        CertificationResponse created = certificationService.saveCertification(
                title, description, isPublic, file, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificationResponse> update(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("isPublic") boolean isPublic,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser principal) throws IOException {

        return certificationService.updateCertification(id, title, description, isPublic, file, principal.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return certificationService.deleteCertification(id, principal.getId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
