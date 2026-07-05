package afam.artidserver.service;

import afam.artidserver.dao.ArtidDAO;
import afam.artidserver.dao.ExternalShareDAO;
import afam.artidserver.dao.InternalShareDAO;
import afam.artidserver.dao.UserDAO;
import afam.artidserver.model.dto.*;
import afam.artidserver.model.entity.Artid;
import afam.artidserver.model.entity.ExternalShare;
import afam.artidserver.model.entity.InternalShare;
import afam.artidserver.model.entity.User;
import afam.artidserver.security.ShareLinkCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import afam.artidserver.storage.StorageService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ExternalShareDAO externalShareDAO;
    private final InternalShareDAO internalShareDAO;
    private final ArtidDAO artidDAO;
    private final UserDAO userDAO;

    private final StorageService storageService;
    private final UserService userService;
    private final EmailService emailService;
    private final ShareLinkCipher shareLinkCipher;

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
        if (shareIds == null || shareIds.isEmpty())
            return;

        // Esegue una singola query di update bulk
        externalShareDAO.disableSharesByIds(userId, shareIds);
    }

    @Transactional
    public void enableAll(Long userId, List<Long> shareIds) {
        if (shareIds == null || shareIds.isEmpty())
            return;

        // Esegue una singola query di update bulk
        externalShareDAO.enableSharesByIds(userId, shareIds);
    }

    @Transactional
    public void extendExpiration(Long userId, Long shareId, OffsetDateTime newExpirationDate) {
        if (newExpirationDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data di scadenza mancante");
        }

        // Ownership: la condivisione deve esistere ed essere dell'utente autenticato
        ExternalShare share = externalShareDAO.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condivisione non trovata"));
        if (!userId.equals(share.getIdCreator())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Condivisione non di tua proprietà");
        }

        // Coerenza: la nuova scadenza deve essere futura e successiva a quella attuale (è una proroga)
        if (!newExpirationDate.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nuova scadenza deve essere futura");
        }
        OffsetDateTime current = share.getExpirationDate();
        if (current != null && !newExpirationDate.isAfter(current)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nuova scadenza deve essere successiva a quella attuale");
        }

        share.setExpirationDate(newExpirationDate);
        externalShareDAO.save(share);
    }

    @Transactional
    public void updateDescription(Long userId, Long shareId, String description) {
        // Ownership: la condivisione deve esistere ed essere dell'utente autenticato
        ExternalShare share = externalShareDAO.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condivisione non trovata"));
        if (!userId.equals(share.getIdCreator())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Condivisione non di tua proprietà");
        }

        share.setDescription(description);
        externalShareDAO.save(share);
    }

    /**
     * Crea una nuova condivisione esterna per un ArtID dell'utente e restituisce id + token del link
     * pubblico. La scadenza è obbligatoria e deve essere futura; la descrizione è opzionale.
     */
    @Transactional
    public CreateExternalShareResponse createExternalShare(Long userId, Long artidId, OffsetDateTime expirationDate,
            String description) {
        if (expirationDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data di scadenza mancante");
        }
        if (!expirationDate.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La scadenza deve essere futura");
        }

        // Ownership: l'ArtID deve esistere ed essere dell'utente autenticato (e non soft-deleted).
        Artid artid = artidDAO.findByIdAndIdUserAndDeletedAtIsNull(artidId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Artid non trovato o non di tua proprietà"));

        // Spring Data JDBC include tutte le colonne mappate nell'INSERT: valorizziamo esplicitamente i
        // default (contatore, stato attivo, data creazione) per non violare i NOT NULL del DB.
        ExternalShare share = new ExternalShare();
        share.setIdArtid(artid.getId());
        share.setIdCreator(userId);
        share.setClickCounter(0);
        share.setIsActive(true);
        share.setExpirationDate(expirationDate);
        share.setCreatedAt(OffsetDateTime.now());
        share.setDescription(description);

        ExternalShare saved = externalShareDAO.save(share);
        return new CreateExternalShareResponse(saved.getId(), shareLinkCipher.encrypt(saved.getId()));
    }

    // Genera il token cifrato del link pubblico per una condivisione dell'utente (owner-only).
    public String generateLink(Long userId, Long shareId) {
        ExternalShare share = externalShareDAO.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condivisione non trovata"));
        if (!userId.equals(share.getIdCreator())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Condivisione non di tua proprietà");
        }
        return shareLinkCipher.encrypt(share.getId());
    }

    /**
     * Apertura del link pubblico: decifra il token, verifica che la condivisione sia attiva e non
     * scaduta, registra la visualizzazione (contatore + prima/ultima visione), alla prima apertura
     * notifica via email il proprietario, e restituisce l'anteprima dell'ArtID collegato. L'anteprima
     * ignora la visibilità dell'ArtID: è il link stesso a concedere l'accesso.
     */
    @Transactional
    public PublicArtidDetailResponse openSharedArtid(String token) {
        long shareId;
        try {
            shareId = shareLinkCipher.decrypt(token);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link non valido.");
        }

        ExternalShare share = externalShareDAO.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link non valido."));

        // Visibile solo se la condivisione è attiva e non scaduta.
        boolean expired = share.getExpirationDate() != null && !share.getExpirationDate().isAfter(OffsetDateTime.now());
        if (expired) {
            throw new ResponseStatusException(HttpStatus.GONE, "Il link non è più valido.");
        }

        if(!Boolean.TRUE.equals(share.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Questo link è stato momentaneamente disattivato. Contatta l’autore per sapere quando tornerà attivo.");
        }


        // ArtID collegato + suo proprietario (l'anteprima usa l'owner, così prescinde dalla visibilità).
        Artid artid = artidDAO.findById(share.getIdArtid())
                .filter(a -> a.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "L'ArtID desiderato non esiste più."));

        PublicArtidDetailResponse detail = userService.getOwnerArtidPreview(artid.getId(), artid.getIdUser())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Non è possibile visualizzare questo ArtID"));

        // Statistiche di visualizzazione: contatore, prima e ultima visione.
        boolean firstOpen = share.getFirstOpened() == null;
        OffsetDateTime now = OffsetDateTime.now();
        share.setClickCounter((share.getClickCounter() == null ? 0 : share.getClickCounter()) + 1);
        if (firstOpen) {
            share.setFirstOpened(now);
        }
        share.setLastOpened(now);
        externalShareDAO.save(share);

        // Alla prima apertura notifica il proprietario dell'ArtID.
        if (firstOpen) {
            notifyOwnerFirstOpen(artid);
        }

        return detail;
    }

    // Email (fire-and-forget) al proprietario alla prima apertura del link di condivisione.
    private void notifyOwnerFirstOpen(Artid artid) {
        userDAO.findById(artid.getIdUser()).ifPresent(owner -> {
            if (owner.getMail() == null || owner.getMail().isBlank()) {
                return;
            }
            String subject = "Il tuo ArtID \"" + artid.getTitle() + "\" è stato visualizzato";
            String body = "Ciao " + (owner.getName() != null ? owner.getName() : "") + ",\n\n"
                    + "il link di condivisione del tuo ArtID \"" + artid.getTitle()
                    + "\" è stato aperto per la prima volta.\n\n"
                    + "Puoi vedere il numero di visualizzazioni nella sezione Condivisioni.\n\n"
                    + "— ArtID";
            emailService.sendText(owner.getMail(), subject, body);
        });
    }

    @Transactional
    public void deleteExternalShares(Long userId, List<Long> shareIds) {
        if (shareIds == null || shareIds.isEmpty())
            return;

        externalShareDAO.deleteSharesByIds(userId, shareIds);
    }

    @Transactional
    public void deleteInternalShares(Long userId, List<Long> shareIds) {
        if (shareIds == null || shareIds.isEmpty())
            return;

        internalShareDAO.deleteSharesByIds(userId, shareIds);
    }

    @Transactional
    public void addInternalShare(Long artidId, String email, Long userId) {
        // Il body arriva come stringa JSON (es. "mail@x.it"): con @RequestBody String è
        // StringHttpMessageConverter a leggerlo, quindi le virgolette di contorno restano nel valore.
        // Le rimuoviamo (più il trim) altrimenti findByMail non troverebbe mai l'utente.
        if (email != null) {
            email = email.trim();
            if (email.length() >= 2 && email.startsWith("\"") && email.endsWith("\"")) {
                email = email.substring(1, email.length() - 1).trim();
            }
        }

        // 1. Validazione formale della mail (controllo preventivo anche lato backend)
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Formato email non valido");
        }

        // 2. Controllo di sicurezza: L'Artid esiste ed è dell'utente che sta provando a
        // condividerlo?
        Artid artid = artidDAO.findByIdAndIdUserAndDeletedAtIsNull(artidId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Artid non trovato o non di tua proprietà"));

        // 3. Verifica se esiste un utente con quella mail nel sistema
        User targetUser = userDAO.findByMail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nessun utente registrato con questa email"));

        // 4. Impedisci di condividere l'Artid con se stessi
        if (targetUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Non puoi condividere un materiale con te stesso");
        }

        InternalShare share = internalShareDAO.findByIdUserFromAndIdUserToAndIdArtid(
                userId,
                targetUser.getId(),
                artid.getId()).map(existingShare -> {
                    // CASO UPDATE:
                    existingShare.setRecipientMail(targetUser.getMail());
                    existingShare.setIsAccepted(Boolean.TRUE.equals(targetUser.getInternalShareEnabled()));
                    existingShare.setCreatedAt(OffsetDateTime.now());

                    return existingShare;
                }).orElseGet(() -> {
                    // CASO INSERT:
                    InternalShare newShare = new InternalShare();
                    newShare.setIdUserFrom(userId);
                    newShare.setIdUserTo(targetUser.getId());
                    newShare.setIdArtid(artid.getId());
                    newShare.setRecipientMail(targetUser.getMail());
                    newShare.setIsAccepted(Boolean.TRUE.equals(targetUser.getInternalShareEnabled()));

                    newShare.setCreatedAt(OffsetDateTime.now());

                    return newShare;
                });

        internalShareDAO.save(share);

        // TODO se accetta condivisioni invia mail

    }

    @Transactional
    public void declineInternalShare(Long idArtid, Long userId) {

        // Eseguiamo l'update iniettando l'utente loggato come destinatario obbligatorio
        int rowsUpdated = internalShareDAO.declineInternalShare(
                userId,
                idArtid);

        // Se rowsUpdated è 0, significa che l'idUserTo non corrispondeva all'utente
        // loggato,
        // oppure che i dati passati non sono validi. Blocchiamo l'operazione.
        if (rowsUpdated == 0) {
            throw new AccessDeniedException(
                    "Non sei autorizzato a rifiutare questa condivisione o la risorsa non esiste.");
        }
    }

    public InternalShareResponse toResponse(InternalShare share) {
        return new InternalShareResponse(
                share.getId(),
                share.getIdUserFrom(),
                share.getIdUserTo(),
                share.getIdArtid(),
                share.getRecipientMail(),
                share.getIsAccepted());
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
                share.getDescription());
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
                presignObjectKey(share.filePath()) // Nuovo valore aggiornato
        );
    }

    private InternalShareArtIDResponse withPresignedPath(InternalShareArtIDResponse share) {
        return new InternalShareArtIDResponse(
                share.id(),
                share.idUserFrom(),
                // share.idUserTo(),
                share.idArtid(),
                share.recipientMail(),
                // share.isAccepted(),
                share.createdAt(),
                share.title(),
                presignObjectKey(share.filePath()) // Nuovo valore aggiornato
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
                share.createdAt(),
                share.title(),
                presignObjectKey(share.filePath()), // Nuovo valore aggiornato
                share.name());
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
        if (objectKey == null || objectKey.isBlank())
            return null;
        return storageService.presignGet(objectKey, Duration.ofSeconds(presignTtlSeconds));
    }
}
