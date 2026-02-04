package nl.kabisa.dashboarding.widget;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EncryptionUtilTest {

    @Test
    public void testEncryptionAndDecryption() throws Exception {
        String plaintext = "{\"apiKey\": \"secret123\", \"token\": \"token456\"}";
        String encryptionKey = "google-calendar-widget";

        // Encrypt the plaintext
        String encrypted = EncryptionUtil.encrypt(plaintext, encryptionKey);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);

        // Decrypt the encrypted text
        String decrypted = EncryptionUtil.decrypt(encrypted, encryptionKey);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptionWithDifferentKeys() throws Exception {
        String plaintext = "{\"apiKey\": \"secret123\"}";
        String key1 = "widget-type-1";
        String key2 = "widget-type-2";

        String encrypted1 = EncryptionUtil.encrypt(plaintext, key1);
        String encrypted2 = EncryptionUtil.encrypt(plaintext, key2);

        // Same plaintext encrypted with different keys should produce different
        // ciphertexts
        assertNotEquals(encrypted1, encrypted2);

        // But decryption with the correct key should work
        assertEquals(plaintext, EncryptionUtil.decrypt(encrypted1, key1));
        assertEquals(plaintext, EncryptionUtil.decrypt(encrypted2, key2));

        // Decryption with wrong key should fail or produce garbage
        assertThrows(Exception.class, () -> {
            String wrongDecryption = EncryptionUtil.decrypt(encrypted1, key2);
            // Just having a different result is not an error in AES, so verify it's not the
            // original
            assertNotEquals(plaintext, wrongDecryption);
        });
    }

    @Test
    public void testNullAndEmptyInputs() throws Exception {
        String encryptionKey = "widget-key";

        // Null input should return null
        assertNull(EncryptionUtil.encrypt(null, encryptionKey));
        assertNull(EncryptionUtil.decrypt(null, encryptionKey));

        // Empty input should return null
        assertNull(EncryptionUtil.encrypt("", encryptionKey));
        assertNull(EncryptionUtil.decrypt("", encryptionKey));
    }
}
