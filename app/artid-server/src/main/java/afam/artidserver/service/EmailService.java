package afam.artidserver.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Invio email transazionali via SMTP (Hostinger). Il mittente è il {@code noreply}
 * istituzionale: deve esistere come mailbox/alias dell'account autenticato sul provider,
 * altrimenti l'invio viene rifiutato o il From riscritto.
 *
 * <p>I metodi sono {@code @Async}: l'SMTP è I/O di rete lento e tenerlo nel thread della richiesta
 * costringerebbe l'utente ad aspettare la consegna prima di vedere la schermata OTP. L'invio è
 * "fire-and-forget" — la riga OTP è già committata dal chiamante prima del dispatch, quindi la
 * verifica funziona anche se la mail arriva con qualche secondo di ritardo. Un fallimento SMTP
 * viene loggato (non propaga al chiamante): l'utente può sempre usare "Invia di nuovo".
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${otp.from-address}")
    private String fromAddress;

    @Value("${otp.from-name:ArtID}")
    private String fromName;

    @Async
    public void sendText(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Invio email di testo a {} fallito", to, e);
        }
    }

    /** Invio asincrono di un'email con corpo HTML (multipart con la sola parte HTML). */
    @Async
    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            logger.error("Invio email HTML a {} fallito", to, e);
        }
    }
}
