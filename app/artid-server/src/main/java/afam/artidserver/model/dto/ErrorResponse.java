package afam.artidserver.model.dto;

/**
 * Corpo JSON di errore restituito dai controller: contiene il solo messaggio da mostrare all'utente.
 * Serve a far arrivare fino al frontend il "reason" delle ResponseStatusException anche sugli
 * endpoint pubblici, dove il forward interno di Spring verso /error viene bloccato dalla security
 * (utente non autenticato) e status + messaggio andrebbero altrimenti persi.
 */
public record ErrorResponse(String message) {
}
