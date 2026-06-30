package afam.artidserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync: l'invio email OTP gira in un thread separato (vedi EmailService) così la risposta
// HTTP non resta agganciata all'I/O SMTP e l'utente arriva subito alla schermata di verifica.
@EnableAsync
@SpringBootApplication
public class ArtidServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtidServerApplication.class, args);
    }

}
