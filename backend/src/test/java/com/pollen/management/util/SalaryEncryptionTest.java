package com.pollen.management.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 薪资数据加密测试：验证 miniCoins (Integer) 和 salaryAmount (BigDecimal) 字段
 * 通过 EncryptedIntegerConverter 和 EncryptedFieldConverter 正确加解密。
 *
 * 由于 JPA Converter 内部委托给 AesEncryptionUtil.encrypt/decrypt（读取环境变量），
 * 这里使用 AesEncryptionUtil 的 key-based 重载方法直接验证转换逻辑的正确性。
 */
class SalaryEncryptionTest {

    private static final byte[] TEST_KEY = "0123456789abcdef".getBytes();

    // --- Integer (miniCoins) encryption round-trip ---

    @Test
    void integerEncryption_roundTrip_shouldPreserveValue() {
        Integer original = 350;
        String encrypted = AesEncryptionUtil.encrypt(original.toString(), TEST_KEY);
        assertNotNull(encrypted);
        assertNotEquals(original.toString(), encrypted);

        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(original, Integer.parseInt(decrypted));
    }

    @Test
    void integerEncryption_roundTrip_zeroValue() {
        Integer original = 0;
        String encrypted = AesEncryptionUtil.encrypt(original.toString(), TEST_KEY);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(original, Integer.parseInt(decrypted));
    }

    @Test
    void integerEncryption_roundTrip_maxMiniCoins() {
        Integer original = 400; // max allowed miniCoins per member
        String encrypted = AesEncryptionUtil.encrypt(original.toString(), TEST_KEY);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(original, Integer.parseInt(decrypted));
    }

    @Test
    void integerEncryption_roundTrip_minMiniCoins() {
        Integer original = 200; // min allowed miniCoins per member
        String encrypted = AesEncryptionUtil.encrypt(original.toString(), TEST_KEY);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(original, Integer.parseInt(decrypted));
    }

    // --- BigDecimal (salaryAmount) encryption round-trip ---

    @Test
    void bigDecimalEncryption_roundTrip_shouldPreserveValue() {
        BigDecimal original = new BigDecimal("300.50");
        String encrypted = AesEncryptionUtil.encrypt(original.toPlainString(), TEST_KEY);
        assertNotNull(encrypted);
        assertNotEquals(original.toPlainString(), encrypted);

        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(0, original.compareTo(new BigDecimal(decrypted)));
    }

    @Test
    void bigDecimalEncryption_roundTrip_zeroValue() {
        BigDecimal original = BigDecimal.ZERO;
        String encrypted = AesEncryptionUtil.encrypt(original.toPlainString(), TEST_KEY);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(0, original.compareTo(new BigDecimal(decrypted)));
    }

    @Test
    void bigDecimalEncryption_roundTrip_maxPoolValue() {
        BigDecimal original = new BigDecimal("2000.00");
        String encrypted = AesEncryptionUtil.encrypt(original.toPlainString(), TEST_KEY);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(0, original.compareTo(new BigDecimal(decrypted)));
    }

    // --- Converter null handling ---

    @Test
    void integerConverter_nullHandling() {
        EncryptedIntegerConverter converter = new EncryptedIntegerConverter();
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void bigDecimalConverter_nullHandling() {
        EncryptedFieldConverter converter = new EncryptedFieldConverter();
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    // --- Encrypted values differ from plaintext ---

    @Test
    void encryptedIntegerDiffersFromPlaintext() {
        String encrypted = AesEncryptionUtil.encrypt("200", TEST_KEY);
        assertNotEquals("200", encrypted);
    }

    @Test
    void encryptedBigDecimalDiffersFromPlaintext() {
        String encrypted = AesEncryptionUtil.encrypt("300.50", TEST_KEY);
        assertNotEquals("300.50", encrypted);
    }

    // --- Different encryptions produce different ciphertexts (random IV) ---

    @Test
    void sameValue_producesDifferentCiphertexts() {
        String enc1 = AesEncryptionUtil.encrypt("200", TEST_KEY);
        String enc2 = AesEncryptionUtil.encrypt("200", TEST_KEY);
        assertNotEquals(enc1, enc2); // AES-GCM uses random IV
    }
}
