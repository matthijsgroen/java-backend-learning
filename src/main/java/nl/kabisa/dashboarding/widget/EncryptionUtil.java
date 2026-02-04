package nl.kabisa.dashboarding.widget;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 16; // 128-bit key for AES

    /**
     * Encrypts a string using AES with a key derived from the provided key string.
     * 
     * @param plaintext the text to encrypt
     * @param keyString the key string (will be padded/truncated to 16 bytes)
     * @return Base64 encoded encrypted string
     */
    public static String encrypt(String plaintext, String keyString) throws Exception {
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
     * provided key string.
     * 
     * @param encryptedText the Base64 encoded encrypted text
     * @param keyString     the key string (must match the one used for encryption)
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedText, String keyString) throws Exception {
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
     * Derives a fixed-size key from the provided key string by padding or
     * truncating to KEY_SIZE bytes.
     */
    private static byte[] deriveKey(String keyString) {
        byte[] keyBytes = keyString.getBytes();
        byte[] key = new byte[KEY_SIZE];

        if (keyBytes.length >= KEY_SIZE) {
            System.arraycopy(keyBytes, 0, key, 0, KEY_SIZE);
        } else {
            System.arraycopy(keyBytes, 0, key, 0, keyBytes.length);
            // Pad with zeros if key is shorter than KEY_SIZE
            for (int i = keyBytes.length; i < KEY_SIZE; i++) {
                key[i] = 0;
            }
        }

        return key;
    }
}
