package afam.artidserver.model.dto;

/**
 * Riepilogo della dashboard in un'unica risposta: evita 5 richieste HTTP separate
 * (ognuna con la propria autenticazione e risoluzione utente) per mostrare 5 numeri.
 */
public record DashboardSummaryResponse(
        long artidCount,
        long resourceCount,
        long certificationCount,
        long shareCount,
        int profileCompletion
) {}
