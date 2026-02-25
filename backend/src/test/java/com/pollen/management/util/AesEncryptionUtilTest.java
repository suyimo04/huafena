package com.pollen.management.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    // 16-byte test key (AES-128)
    private static final byte[] TEST_KEY_16 = "0123456789abcdef".getBytes();
    // 32-byte test key (AES-256)
    private static final byte[] TEST_KEY_32 = "0123456789abcdef0123456789abcdef".getBytes();

    @Test
    void encrypt_decrypt_roundTrip_shouldReturnOriginalText() {
        String plainText = "Hello, 花粉小组!";
        String encrypted = AesEncryptionUtil.encrypt(plainText, TEST_KEY_16);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY_16);
        assertEquals(plainText, decrypted);
    }

    @Test
    void encrypt_decrypt_withAes256Key_shouldWork() {
        String plainText = "AES-256 test data";
        String encrypted = AesEncryptionUtil.encrypt(plainText, TEST_KEY_32);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY_32);
        assertEquals(plainText, decrypted);
    }

    @Test
    void encrypt_shouldReturnDifferentCiphertextEachTime() {
        String plainText = "same input";
        String encrypted1 = AesEncryptionUtil.encrypt(plainText, TEST_KEY_16);
        String encrypted2 = AesEncryptionUtil.encrypt(plainText, TEST_KEY_16);
        // GCM uses random IV, so ciphertexts should differ
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_shouldReturnBase64EncodedString() {
        String encrypted = AesEncryptionUtil.encrypt("test", TEST_KEY_16);
        assertNotNull(encrypted);
        // Should be valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
    }

    @Test
    void encrypt_shouldReturnNullForNullInput() {
        assertNull(AesEncryptionUtil.encrypt(null, TEST_KEY_16));
    }

    @Test
    void decrypt_shouldReturnNullForNullInput() {
        assertNull(AesEncryptionUtil.decrypt(null, TEST_KEY_16));
    }

    @Test
    void encrypt_shouldNotEqualPlainText() {
        String plainText = "sensitive data";
        String encrypted = AesEncryptionUtil.encrypt(plainText, TEST_KEY_16);
        assertNotEquals(plainText, encrypted);
    }

    @Test
    void decrypt_withWrongKey_shouldThrowException() {
        String encrypted = AesEncryptionUtil.encrypt("secret", TEST_KEY_16);
        byte[] wrongKey = "fedcba9876543210".getBytes();
        assertThrows(BusinessException.class,
                () -> AesEncryptionUtil.decrypt(encrypted, wrongKey));
    }

    @Test
    void decrypt_withCorruptedData_shouldThrowException() {
        assertThrows(Exception.class,
                () -> AesEncryptionUtil.decrypt("not-valid-base64!!!", TEST_KEY_16));
    }

    @Test
    void encrypt_decrypt_emptyString_shouldWork() {
        String encrypted = AesEncryptionUtil.encrypt("", TEST_KEY_16);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY_16);
        assertEquals("", decrypted);
    }

    @Test
    void encrypt_decrypt_bigDecimalValue_shouldPreservePrecision() {
        String value = "12345.67890";
        String encrypted = AesEncryptionUtil.encrypt(value, TEST_KEY_16);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY_16);
        assertEquals(new BigDecimal(value), new BigDecimal(decrypted));
    }

    @Test
    void encrypt_decrypt_unicodeContent_shouldWork() {
        String plainText = "密码：P@ssw0rd!#$%^&*()_+中文テスト";
        String encrypted = AesEncryptionUtil.encrypt(plainText, TEST_KEY_16);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY_16);
        assertEquals(plainText, decrypted);
    }

    @Test
    void encrypt_decrypt_longString_shouldWork() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("abcdefghij");
        }
        String longText = sb.toString();
        String encrypted = AesEncryptionUtil.encrypt(longText, TEST_KEY_16);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY_16);
        assertEquals(longText, decrypted);
    }
}
