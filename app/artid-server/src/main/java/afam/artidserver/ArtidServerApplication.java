package afam.artidserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;


// TODO Rimuovi exclude una volta impostato D ataSource
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ArtidServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtidServerApplication.class, args);
    }

}
