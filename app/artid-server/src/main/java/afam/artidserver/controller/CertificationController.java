package afam.artidserver.controller;

import afam.artidserver.model.dto.CertificationResponse;
import afam.artidserver.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CertificationResponse>> getAll() {
        // Sostituisci l'ID utente mockato (1L) con il recupero dell'utente autenticato dal tuo pacchetto security
        Long currentUserId = 1L;
        return ResponseEntity.ok(certificationService.findAllCertifications(currentUserId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificationResponse> create(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("isPublic") boolean isPublic,
            @RequestParam("file") MultipartFile file) throws IOException {

        Long currentUserId = 1L;
        CertificationResponse created = certificationService.saveCertification(title, description, isPublic, file, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificationResponse> update(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("isPublic") boolean isPublic,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        return ResponseEntity.ok(certificationService.updateCertification(id, title, description, isPublic, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        certificationService.deleteCertification(id);
        return ResponseEntity.noContent().build();
    }
}