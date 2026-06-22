package afam.artidserver.service;

import afam.artidserver.dao.CertificationDAO;
import afam.artidserver.model.entity.Certification;
import afam.artidserver.model.dto.CertificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationDAO certificationDAO;

    public long countByUser(Long userId) {
        return certificationDAO.countByIdUser(userId);
    }

    // 1. Recupera tutte le certificazioni dell'utente loggato
    public List<CertificationResponse> findAllCertifications(Long userId) {
        List<Certification> certifications = certificationDAO.findAllByIdUser(userId);

        return certifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // 2. Salva una nuova certificazione + File
    @Transactional
    public CertificationResponse saveCertification(String title, String description, Boolean isPublic, MultipartFile file, Long userId) throws IOException {
        Long generatedFileId = System.currentTimeMillis();

        Certification certification = new Certification();
        certification.setIdUser(userId);
        certification.setIdFile(generatedFileId);
        certification.setTitle(title);
        certification.setDescription(description);
        certification.setIsPublic(isPublic);

        Certification saved = certificationDAO.save(certification);

        return convertToResponse(saved);
    }

    // 3. Aggiorna una certificazione esistente
    @Transactional
    public CertificationResponse updateCertification(Long id, String title, String description, Boolean isPublic, MultipartFile file) throws IOException {
        Certification existing = certificationDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificazione non trovata"));

        existing.setTitle(title);
        existing.setDescription(description);
        existing.setIsPublic(isPublic);

        if (file != null && !file.isEmpty()) {
            Long newFileId = System.currentTimeMillis();
            existing.setIdFile(newFileId);
        }

        Certification updated = certificationDAO.save(existing);
        return convertToResponse(updated);
    }

    // 4. Elimina la certificazione
    @Transactional
    public void deleteCertification(Long id) {
        Certification existing = certificationDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificazione non trovata"));

        certificationDAO.delete(existing);
    }

    // 5. Helper per convertire l'Entity nel DTO richiesto da Svelte
    private CertificationResponse convertToResponse(Certification cert) {
        return CertificationResponse.builder()
                .id(cert.getId())
                .title(cert.getTitle())
                .description(cert.getDescription())
                .isPublic(cert.getIsPublic() != null ? cert.getIsPublic() : false)
                .extension("pdf")
                .fileSize(799744L)
                .build();
    }
}