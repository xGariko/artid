package afam.artidserver.model.dto;

// Restituito alla creazione di una condivisione esterna: id del record + token del link pubblico
// già cifrato, così il client può copiarlo e aprirlo senza una seconda chiamata.
public record CreateExternalShareResponse(
        Long id,
        String token) {
}
