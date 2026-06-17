// package afam.artidserver.service;

// import java.util.List;
// import java.util.Random;

// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
// import org.springframework.web.server.ResponseStatusException;
// import org.springframework.beans.factory.annotation.Value;

// import afam.artidserver.dao.TagDAO;
// import afam.artidserver.model.dto.TagResponse;
// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class TagService {

// private final TagDAO tagDAO;
// @Value("#{'${app.security.blacklisted-tags}'.split(',')}")
// private List<String> blacklist;

// private final Random random = new Random();

// /**
// * Recupera tutti i tag associati a uno specifico utente.
// */
// public List<TagResponse> findByUser(Long userId) {
// return tagDAO.findByUserId(userId)
// .stream()
// .map(TagService::toResponse)
// .toList();
// }

// /**
// * Crea un nuovo tag e lo assegna all'utente loggato.
// */
// public TagResponse create(String title, String color, Long userId) {
// if (tagDAO.existsByTitleAndUserId(title, userId)) {
// throw new ResponseStatusException(
// HttpStatus.CONFLICT,
// "Hai già creato un tag con il nome: " + title);
// }

// boolean isViolated = blacklist.stream()
// .anyMatch(forbiddenWord ->
// title.trim().toLowerCase().contains(forbiddenWord.toLowerCase().trim()));

// if (isViolated) {
// throw new ResponseStatusException(
// HttpStatus.BAD_REQUEST,
// "Il nome del tag contiene termini non consentiti dalle linee guida
// istituzionali." + title);
// }

// String finalColor = (color == null || color.isBlank()) ?
// generateRandomColor() : color;

// Tag tag = new Tag();
// tag.setIdUser(userId);
// tag.setTitle(title);
// tag.setColor(finalColor);
// tag.setCreatedAt(now);
// // 3. Salviamo l'entità sul database
// return toResponse(tagDAO.save(tag));

// }

// private static TagResponse toResponse(Tag t) {
// return new TagResponse(t.getId(), t.getTitle(), t.getColor());
// }

// /**
// * Metodo helper privato per generare una stringa HEX a 6 cifre (es. "ff5733")
// */
// private String generateRandomColor() {
// // Limitando il range tra 50 e 200 otteniamo tonalità pastello sature,
// // evitando il nero totale (0) e il bianco totale (255)
// int r = random.nextInt(151);
// int g = random.nextInt(151);
// int b = random.nextInt(151);

// // %02x significa: converti in esadecimale, minuscolo, usando sempre almeno 2
// // cifre (es. 9 diventa 09)
// return String.format("%02x%02x%02x", r, g, b);
// }
// }
