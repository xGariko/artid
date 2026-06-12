package afam.artidserver.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
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
    private boolean internalShareEnabled;
}
