package afam.artidserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Invio email transazionali via SMTP (Hostinger). Il mittente è il {@code noreply}
 * istituzionale: deve esistere come mailbox/alias dell'account autenticato sul provider,
 * altrimenti l'invio viene rifiutato o il From riscritto.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${otp.from-address}")
    private String fromAddress;

    @Value("${otp.from-name:ArtID}")
    private String fromName;

    public void sendText(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + fromAddress + ">");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
