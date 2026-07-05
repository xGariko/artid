package afam.artidserver.service;

import java.util.List;
import java.util.Random;
import java.awt.Color;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import afam.artidserver.dao.TagDAO;
import afam.artidserver.model.dto.TagResponse;
import afam.artidserver.model.entity.Tag;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagDAO tagDAO;
    @Value("${app.security.blacklist}")
    private List<String> blacklist;

    private final Random random = new Random();

    /**
     * Recupera tutti i tag associati a uno specifico utente.
     */
    public List<TagResponse> findByUser(Long userId) {
        return tagDAO.findByIdUser(userId)
                .stream()
                .map(TagService::toResponse)
                .toList();
    }

    /**
     * Crea un nuovo tag e lo assegna all'utente loggato.
     */
    public TagResponse create(String title, Long userId) {

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il titolo del tag non può essere vuoto");
        }
        String cleanedTitle = title.trim();

        if (tagDAO.existsByTitleAndIdUser(cleanedTitle, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hai già creato un tag con il nome: " + cleanedTitle);
        }

        // 1. Normalizzazione della stringa:
        // Rimuove gli spazi all'inizio/fine, converte in minuscolo e riduce gli spazi
        // multipli interni a uno solo
        String normalizedTitle = title.trim().toLowerCase().replaceAll("\\s+", " ");

        // 2. Controllo Blacklist
        boolean isViolated = blacklist.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(forbiddenWord -> normalizedTitle.contains(forbiddenWord));

        if (isViolated) {
            throw new IllegalArgumentException(
                    "Il nome del tag contiene termini non consentiti dalle linee guida istituzionali.");
        }

        String color = generateRandomColor();
        Tag tag = new Tag();
        tag.setIdUser(userId);
        tag.setTitle(title);
        tag.setColor(color);

        return toResponse(tagDAO.save(tag));

    }

    public List<TagResponse> findByArtid(Long artidId, Long userId) {
        return toResponses(tagDAO.findByArtidForUser(artidId, userId));
    }

    private List<TagResponse> toResponses(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .map(TagService::toResponse)
                .toList();
    }

    private static TagResponse toResponse(Tag t) {
        return new TagResponse(t.getId(), t.getTitle(), t.getColor());
    }

    /**
     * Metodo helper privato per generare una stringa HEX a 6 cifre (es. "ff5733")
     */
    private String generateRandomColor() {
        float hue = random.nextFloat(); // Varia la tonalità (rosso, verde, blu, giallo...)
        float saturation = 0.85f; // 85% di saturazione: colore pieno e vivido
        float brightness = 0.90f; // 90% di luminosità: splendente e visibile

        // Converte HSB in un oggetto Color standard di Java
        Color color = Color.getHSBColor(hue, saturation, brightness);

        // Converte i canali RGB in una stringa HEX (es. "ff33a1")
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
