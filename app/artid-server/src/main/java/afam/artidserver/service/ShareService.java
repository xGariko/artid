package afam.artidserver.service;

import afam.artidserver.dao.ExternalShareDAO;
import afam.artidserver.dao.InternalShareDAO;
import afam.artidserver.model.dto.*;
import afam.artidserver.model.entity.ExternalShare;
import afam.artidserver.model.entity.InternalShare;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import afam.artidserver.storage.StorageService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ExternalShareDAO externalShareDAO;
    private final InternalShareDAO internalShareDAO;

    private final StorageService storageService;

    @Value("${supabase.s3.bucket}")
    private String thumbnailBucket;

    @Value("${supabase.s3.presign-ttl-seconds}")
    private long presignTtlSeconds;

    public long countByUser(Long userId) {
        return externalShareDAO.countByIdCreator(userId) + internalShareDAO.countByIdUserFrom(userId);
    }

    public long countInternalByUser(Long userId) {
        return internalShareDAO.countByIdUserFrom(userId);
    }

    public long countExternalByUser(Long userId) {
        return externalShareDAO.countByIdCreator(userId);
    }

    public List<InternalShareArtIDResponse> getInternalFromUser(Long userId) {
        return internalShareDAO.getInternalSharesFromUserID(userId)
                .stream()
                .map(this::withPresignedPath)
                .toList();
    }

    public List<InternalShareArtIDExtendedResponse> getInternalToUser(Long userId) {
        return internalShareDAO.getInternalSharesToUserID(userId)
                .stream()
                .map(this::withPresignedPath)
                .toList();
    }

    public List<ExternalShareArtIDResponse> getExternalByUser(Long userId) {
        return externalShareDAO.getExternalSharesByUserID(userId)
                .stream()
                .map(this::withPresignedPath)
                .toList();
    }

    @Transactional
    public void disableAll(Long userId, List<Long> shareIds) {
        if (shareIds == null || shareIds.isEmpty()) return;

        // Esegue una singola query di update bulk
        externalShareDAO.disableSharesByIds(userId, shareIds);
    }

    @Transactional
    public void enableAll(Long userId, List<Long> shareIds) {
        if (shareIds == null || shareIds.isEmpty()) return;

        // Esegue una singola query di update bulk
        externalShareDAO.enableSharesByIds(userId, shareIds);
    }

    @Transactional
    public void deleteAll(Long userId, List<Long> shareIds) {
        if (shareIds == null || shareIds.isEmpty()) return;

        // Esegue una singola query di update bulk
        externalShareDAO.deleteSharesByIds(userId, shareIds);
    }

    public InternalShareResponse toResponse(InternalShare share) {
        return new InternalShareResponse(
            share.getId(),
            share.getIdUserFrom(),
            share.getIdUserTo(),
            share.getIdArtid(),
            share.getRecipientMail(),
            share.getIsAccepted()
        );
    }

    public ExternalShareResponse toResponse(ExternalShare share) {
        return new ExternalShareResponse(
                share.getId(),
                share.getIdArtid(),
                share.getIdCreator(),
                share.getClickCounter(),
                share.getIsActive(),
                share.getExpirationDate(),
                share.getLastOpened(),
                share.getCreatedAt(),
                share.getFirstOpened(),
                share.getDescription()
        );
    }

    private ExternalShareArtIDResponse withPresignedPath(ExternalShareArtIDResponse share) {
        return new ExternalShareArtIDResponse(
                share.id(),
                share.idArtid(),
                share.idCreator(),
                share.clickCounter(),
                share.isActive(),
                share.expirationDate(),
                share.lastOpened(),
                share.createdAt(),
                share.firstOpened(),
                share.description(),
                share.title(),
                presignObjectKey(share.file_path()) // Nuovo valore aggiornato
        );
    }

    private InternalShareArtIDResponse withPresignedPath(InternalShareArtIDResponse share) {
        return new InternalShareArtIDResponse(
                share.id(),
                share.idUserFrom(),
                share.idUserTo(),
                share.idArtid(),
                share.recipientMail(),
                share.isAccepted(),
                share.title(),
                presignObjectKey(share.file_path()) // Nuovo valore aggiornato
        );
    }

    private InternalShareArtIDExtendedResponse withPresignedPath(InternalShareArtIDExtendedResponse share) {
        return new InternalShareArtIDExtendedResponse(
                share.id(),
                share.idUserFrom(),
                share.idUserTo(),
                share.idArtid(),
                share.recipientMail(),
                share.isAccepted(),
                share.title(),
                presignObjectKey(share.file_path()), // Nuovo valore aggiornato
                share.name()
        );
    }


    public List<ExternalShareResponse> toResponsesExt(List<ExternalShare> shares) {
        List<ExternalShareResponse> responses = new ArrayList<>();
        for (ExternalShare share : shares) {
            responses.add(toResponse(share));
        }
        return responses;
    }

    public List<InternalShareResponse> toResponsesInt(List<InternalShare> shares) {
        List<InternalShareResponse> responses = new ArrayList<>();
        for (InternalShare share : shares) {
            responses.add(toResponse(share));
        }
        return responses;
    }

    private String presignObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        return storageService.presignGet(objectKey, Duration.ofSeconds(presignTtlSeconds));
    }
}
