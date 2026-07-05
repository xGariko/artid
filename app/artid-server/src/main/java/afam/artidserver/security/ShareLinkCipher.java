package afam.artidserver.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Cifratura simmetrica deterministica (AES-CBC) dell'id di una condivisione esterna.
 * Produce sempre lo stesso token per lo stesso shareId derivando l'IV dall'id stesso.
 */
@Component
public class ShareLinkCipher {

    // Modificato in AES/CBC/PKCS5Padding per supportare la cifratura a blocchi con IV deterministico
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16; // AES richiede un IV di 16 byte per la modalità CBC

    private final SecretKeySpec key;

    public ShareLinkCipher(@Value("${share.link.secret}") String secret) {
        this.key = deriveKey(secret);
    }

    /** Cifra l'id della condivisione in un token URL-safe deterministico (IV || ciphertext). */
    public String encrypt(long shareId) {
        try {
            byte[] plaintext = ByteBuffer.allocate(Long.BYTES).putLong(shareId).array();

            // Genera l'IV in modo deterministico partendo dall'ID
            byte[] iv = generateDeterministicIv(plaintext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile generare il token di condivisione", e);
        }
    }

    /** Decifra il token nell'id della condivisione. */
    public long decrypt(String token) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(token);
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return ByteBuffer.wrap(plaintext).getLong();
        } catch (Exception e) {
            throw new IllegalArgumentException("Token di condivisione non valido", e);
        }
    }

    /** Genera un IV stabile di 16 byte calcolando l'hash SHA-256 dei dati in ingresso. */
    private byte[] generateDeterministicIv(byte[] plaintext) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(plaintext);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(hash, 0, iv, 0, IV_LENGTH); // Estrae i primi 16 byte del digest
        return iv;
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
