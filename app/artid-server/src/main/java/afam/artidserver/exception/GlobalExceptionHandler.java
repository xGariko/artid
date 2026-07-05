package afam.artidserver.exception;

import afam.artidserver.model.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converte le ResponseStatusException in un corpo JSON { "message": ... } prodotto DENTRO il
 * dispatch della richiesta. Senza questo, Spring gestisce l'eccezione con response.sendError() e
 * forwarda a /error: su un endpoint pubblico (es. /api/shares/public/*) quel secondo passaggio
 * ricade sulla security, che con utente anonimo blocca /error e sostituisce status + messaggio con
 * un 403 vuoto. Gestendo l'eccezione qui, l'oggetto con il messaggio "sale" fino al controller e
 * viene restituito tale e quale, così il frontend può mostrare l'errore reale (link scaduto,
 * disattivato, ArtID rimosso, ...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(ex.getReason()));
    }
}
