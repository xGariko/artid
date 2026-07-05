package afam.artidserver.model.dto;

import afam.artidserver.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String surname;
    private LocalDate birthdate;
    private String birthplace;
    private String address;
    private String biography;
    private String linkedinId;
    private String facebookId;
    private String instagramId;
    private String profession;
    private Boolean isPublic;
    private String phone;
    private String businessEmail;
    // Presigned GET URL della foto profilo (null se assente): l'<img> punta diretto a Supabase.
    private String propicUrl;
    private Boolean internalShareEnabled;
    // true se l'account è verificato via SPID (spidCode valorizzato): in Gestione Profilo il
    // pulsante mostra "SPID COLLEGATO" invece di "Associa SPID" (RAD, caso d'uso COL_SPID).
    private Boolean spidLinked;
    // true se l'account ha una password reale. Guida la modale "Chiudi account": se false (Membro
    // nato da SPID senza password) l'eliminazione chiede le credenziali SPID invece della password.
    private Boolean passwordSet;

    public static ProfileResponse from(User user, String propicUrl) {
        return new ProfileResponse(
                user.getId(),
                user.getMail(),
                user.getName(),
                user.getSurname(),
                user.getBirthdate(),
                user.getBirthplace(),
                user.getAddress(),
                user.getBiography(),
                user.getLinkedinId(),
                user.getFacebookId(),
                user.getInstagramId(),
                user.getProfession(),
                user.getIsPublic(),
                user.getPhone(),
                user.getBusinessEmail(),
                propicUrl,
                user.getInternalShareEnabled(),
                user.getSpidCode() != null && !user.getSpidCode().isBlank(),
                // Difensivo: una riga senza flag (non dovrebbe capitare, la colonna è NOT NULL) viene
                // trattata come "ha password", così l'eliminazione ricade sul flusso classico.
                !Boolean.FALSE.equals(user.getPasswordSet())
        );
    }
}
