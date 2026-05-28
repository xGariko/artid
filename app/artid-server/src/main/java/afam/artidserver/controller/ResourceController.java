package afam.artidserver.controller;

import afam.artidserver.model.dto.CountResponse;
import afam.artidserver.model.dto.ResourceResponse;
import afam.artidserver.model.dto.ResourceUpsertRequest;
import afam.artidserver.model.dto.UploadIntentRequest;
import afam.artidserver.model.dto.UploadIntentResponse;
import afam.artidserver.model.entity.User;
import afam.artidserver.service.ResourceService;
import afam.artidserver.service.UserService;
import afam.artidserver.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
    private final ObjectStorageService objectStorageService;

    // Genera un URL presigned per upload diretto su MinIO. Il browser fa PUT sul URL ritornato
    // col body del file, MinIO valida la firma e accetta il body. Poi il browser chiama POST
    // /api/resources passando l'objectKey ricevuto qui dentro ResourceUpsertRequest.
    @PostMapping("/upload-intent")
    public ResponseEntity<UploadIntentResponse> uploadIntent(
            @RequestBody UploadIntentRequest request,
            Authentication authentication
    ) throws Exception {
        userService.findByMail(authentication.getName()).orElseThrow();
        String objectKey = objectStorageService.newObjectKey(request.fileName());
        ObjectStorageService.PresignedUpload presigned = objectStorageService.presignedPut(objectKey, request.mimeType());
        return ResponseEntity.ok(new UploadIntentResponse(
                presigned.url(),
                presigned.objectKey(),
                presigned.expiresInSeconds()
        ));
    }

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
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication authentication) throws Exception {
        User user = userService.findByMail(authentication.getName()).orElseThrow();
        var fileOpt = resourceService.findFileByResourceId(id, user.getId());
        if (fileOpt.isEmpty()) return ResponseEntity.notFound().build();

        var file = fileOpt.get();
        MediaType contentType = file.getMimeType() != null
                ? MediaType.parseMediaType(file.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        String filename = file.getFileName() != null ? file.getFileName() : "file";
        String disposition = "inline; filename=\"" + filename + "\"";

        // File "nuovo" su MinIO: stream con content-length corretto preso da stat() così il
        // browser può mostrare progresso e Spring non bufferizza tutto in RAM.
        if (file.getFilePath() != null) {
            ObjectStorageService.ObjectStat stat = objectStorageService.stat(file.getFilePath());
            Resource body = new InputStreamResource(objectStorageService.get(file.getFilePath()));
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(stat.size())
                    .header("Content-Disposition", disposition)
                    .body(body);
        }

        // Fallback legacy: file con blob bytea ancora non migrato.
        if (file.getBlob() != null) {
            Resource body = new ByteArrayResource(file.getBlob());
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(file.getBlob().length)
                    .header("Content-Disposition", disposition)
                    .body(body);
        }

        return ResponseEntity.notFound().build();
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
