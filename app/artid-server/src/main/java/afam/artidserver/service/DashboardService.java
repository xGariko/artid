package afam.artidserver.service;

import afam.artidserver.model.dto.DashboardSummaryResponse;
import afam.artidserver.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArtidService artidService;
    private final ResourceService resourceService;
    private final CertificationService certificationService;
    private final ShareService shareService;
    private final ProfileService profileService;

    /**
     * Aggrega i conteggi della dashboard. La transazione read-only fa viaggiare tutti i
     * count su UNA sola connessione/lease del pooler Supabase, invece di pagare un
     * checkout di connessione (e una risoluzione utente) per ogni endpoint separato.
     * La percentuale di completamento profilo è calcolata in memoria sull'utente già
     * caricato: zero query aggiuntive.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(User user) {
        Long userId = user.getId();
        return new DashboardSummaryResponse(
                artidService.countByUser(userId),
                resourceService.countByUser(userId),
                certificationService.countByUser(userId),
                shareService.countByUser(userId),
                profileService.completionPercentage(user)
        );
    }
}
