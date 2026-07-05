package afam.artidserver.service;

import afam.artidserver.model.mock.MockSpidIdentity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Provider di identità digitale FINTO. Sostituisce la federazione SPID/SAML reale del caso d'uso RAD
 * "Autenticazione con Identità digitale": invece di contattare un vero Identity Provider, autentica
 * le credenziali contro una lista di 10 identità hardcodate. NON interroga il DB — i dati anagrafici
 * qui sono finti; il provisioning dell'utente reale avviene a valle in {@link SpidAuthService}.
 *
 * <p>Per semplicità di test tutte le identità condividono la stessa password
 * ({@link #SHARED_PASSWORD}) e il provider scelto a video è puramente cosmetico: qualsiasi provider
 * autentica qualsiasi identità.</p>
 */
@Component
public class MockSpidIdentityProvider {

    // Password unica per tutte le identità mock: rende immediato il test manuale del flusso.
    public static final String SHARED_PASSWORD = "Spid!2024";

    // I codici fiscali sono verosimili ma non garantiti checksum-validi: è un mock.
    private static final List<MockSpidIdentity> IDENTITIES = List.of(
            new MockSpidIdentity("RSSMRA85M01H501Z", SHARED_PASSWORD, "Mario", "Rossi", "mario.rossi@spid.test", LocalDate.of(1985, 8, 1), "Roma"),
            new MockSpidIdentity("VRDLGI90A41F205X", SHARED_PASSWORD, "Luigi", "Verdi", "luigi.verdi@spid.test", LocalDate.of(1990, 1, 1), "Milano"),
            new MockSpidIdentity("BNCGIA88T50L219K", SHARED_PASSWORD, "Giulia", "Bianchi", "giulia.bianchi@spid.test", LocalDate.of(1988, 12, 10), "Torino"),
            new MockSpidIdentity("FRRLCU92E15F839W", SHARED_PASSWORD, "Luca", "Ferrari", "luca.ferrari@spid.test", LocalDate.of(1992, 5, 15), "Napoli"),
            new MockSpidIdentity("RMNSFN95B20D612Y", SHARED_PASSWORD, "Stefano", "Romano", "stefano.romano@spid.test", LocalDate.of(1995, 2, 20), "Firenze"),
            new MockSpidIdentity("CLOMRT93P55G273H", SHARED_PASSWORD, "Marta", "Colombo", "marta.colombo@spid.test", LocalDate.of(1993, 9, 15), "Palermo"),
            new MockSpidIdentity("GLLFNC87R12A944J", SHARED_PASSWORD, "Francesco", "Galli", "francesco.galli@spid.test", LocalDate.of(1987, 10, 12), "Bologna"),
            new MockSpidIdentity("CNTLRA91D63C351Q", SHARED_PASSWORD, "Laura", "Conti", "laura.conti@spid.test", LocalDate.of(1991, 4, 23), "Genova"),
            new MockSpidIdentity("MRNDVD89H05L736P", SHARED_PASSWORD, "Davide", "Marino", "davide.marino@spid.test", LocalDate.of(1989, 6, 5), "Venezia"),
            new MockSpidIdentity("GRECHR94S48E506R", SHARED_PASSWORD, "Chiara", "Greco", "chiara.greco@spid.test", LocalDate.of(1994, 11, 8), "Bari"),
            new MockSpidIdentity("VNIGRL04L20G273B", SHARED_PASSWORD, "Gabriele", "Iovino", "gabrieleiovino839@gmail.com", LocalDate.of(1994, 11, 8), "Bari")
    );

    /**
     * Autentica le credenziali presso il provider mock. {@code Optional.empty()} = autenticazione
     * fallita: il caso d'uso lo tratta come "impossibile contattare il provider". Confronto
     * dell'username (codice fiscale) case-insensitive, password esatta.
     */
    public Optional<MockSpidIdentity> authenticate(String username, String password) {
        if (password == null) {
            return Optional.empty();
        }
        return findByUsername(username).filter(identity -> identity.password().equals(password));
    }

    /**
     * Rilegge un'identità dal solo username (codice fiscale), senza password: serve allo step di
     * verifica OTP, dove l'identità è già stata autenticata allo step precedente e va solo ri-risolta
     * per il merge dei dati. {@code Optional.empty()} = username sconosciuto.
     */
    public Optional<MockSpidIdentity> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String normalized = username.trim();
        return IDENTITIES.stream()
                .filter(identity -> identity.username().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
