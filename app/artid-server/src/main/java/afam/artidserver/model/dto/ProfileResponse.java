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
    private byte[] propic;
    private Boolean internalShareEnabled;

    public static ProfileResponse from(User user) {
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
                user.getPropic(),
                user.getInternalShareEnabled()
        );
    }
}
