package afam.artidserver.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifratura simmetrica (AES-GCM) dell'id di una condivisione esterna, per costruire il token del
 * link pubblico. Il token è autonomo: non va memorizzato, la sola chiave lato server permette di
 * risalire all'id. La chiave a 256 bit è derivata via SHA-256 dal segreto configurato, così una
 * qualsiasi stringa (property/variabile d'ambiente) è utilizzabile senza vincoli di lunghezza.
 */
@Component
public class ShareLinkCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;        // dimensione IV standard per GCM
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public ShareLinkCipher(@Value("${share.link.secret}") String secret) {
        this.key = deriveKey(secret);
    }

    /** Cifra l'id della condivisione in un token URL-safe (IV || ciphertext, Base64 senza padding). */
    public String encrypt(long shareId) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(shareId).array());

            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile generare il token di condivisione", e);
        }
    }

    /** Decifra il token nell'id della condivisione. IllegalArgumentException se manomesso o malformato. */
    public long decrypt(String token) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(token);
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return ByteBuffer.wrap(cipher.doFinal(ciphertext)).getLong();
        } catch (Exception e) {
            throw new IllegalArgumentException("Token di condivisione non valido", e);
        }
    }

    private static SecretKeySpec deriveKey(String secret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile derivare la chiave di cifratura del link", e);
        }
    }
}
