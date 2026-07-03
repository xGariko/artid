package afam.artidserver.model.dto;

public record InternalShareArtIDExtendedResponse(
        Long id,
        Long idUserFrom,
        Long idUserTo,
        Long idArtid,
        String recipientMail,
        Boolean isAccepted,
        String title,
        String file_path,
        String name
) {
}
