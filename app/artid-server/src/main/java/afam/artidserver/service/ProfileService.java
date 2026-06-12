package afam.artidserver.service;

import afam.artidserver.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.List;

@Service
public class ProfileService {

    private static final List<Function<User, Object>> OPTIONAL_FIELDS = List.of(
            User::getBirthdate,
            User::getBirthplace,
            User::getAddress,
            User::getBiography,
            User::getLinkedinId,
            User::getFacebookId,
            User::getInstagramId,
            User::getProfession,
            User::getPhone,
            User::getBusinessEmail
    );

    public int completionPercentage(User user) {
        long filled = OPTIONAL_FIELDS.stream()
                .map(getter -> getter.apply(user))
                .filter(ProfileService::isFilled)
                .count();
        return (int) Math.round((filled * 100.0) / OPTIONAL_FIELDS.size());
    }

    private static boolean isFilled(Object value) {
        if (value == null) return false;
        if (value instanceof String s) return !s.trim().isEmpty();
        return true;
    }
}
