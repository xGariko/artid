package afam.artidserver.model.dto;

/**
 * Payload di creazione di un ArtID. Per ora solo il titolo: id_user viene dal principal
 * autenticato (mai dal client), gli altri campi prendono i default applicativi.
 */
public record ArtidCreateRequest(String title) {
}
