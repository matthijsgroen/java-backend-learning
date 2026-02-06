package nl.kabisa.dashboarding.widget;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128; // 128-bit key for AES
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final String SALT = "widget-encryption-salt"; // Fixed salt for consistent key derivation

    /**
     * Encrypts a string using AES with a key derived from the provided key string
     * using PBKDF2.
     * 
     * @param plaintext the text to encrypt
     * @param keyString the key string (will be hashed using PBKDF2)
     * @return Base64 encoded encrypted string
     * @throws GeneralSecurityException if encryption fails
     */
    public static String encrypt(String plaintext, String keyString) throws GeneralSecurityException {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        byte[] decodedKey = deriveKey(keyString);
        SecretKeySpec keySpec = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts a Base64-encoded string using AES with a key derived from the
     * provided key string using PBKDF2.
     * 
     * @param encryptedText the Base64 encoded encrypted text
     * @param keyString     the key string (must match the one used for encryption)
     * @return decrypted plaintext string
     * @throws GeneralSecurityException if decryption fails
     */
    public static String decrypt(String encryptedText, String keyString) throws GeneralSecurityException {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        byte[] decodedKey = deriveKey(keyString);
        SecretKeySpec keySpec = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decodedBytes);
        return new String(decrypted);
    }

    /**
     * Derives a fixed-size key from the provided key string using PBKDF2.
     * Uses a fixed salt and iteration count for deterministic key derivation.
     * 
     * @throws GeneralSecurityException if key derivation fails
     */
    private static byte[] deriveKey(String keyString) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(
                keyString.toCharArray(),
                SALT.getBytes(),
                ITERATIONS,
                KEY_SIZE);

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }
}
