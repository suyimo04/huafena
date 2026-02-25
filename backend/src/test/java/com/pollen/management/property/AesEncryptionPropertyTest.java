package com.pollen.management.property;

import com.pollen.management.util.AesEncryptionUtil;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for AES encryption round-trip consistency.
 *
 * Property 39: AES 加密往返一致性
 * For any sensitive data value (salary amount, SMTP password),
 * encrypting with AES then decrypting should return the original value.
 *
 * **Validates: Requirements 13.7, 17.5**
 */
class AesEncryptionPropertyTest {

    // ========================================================================
    // Property 39: AES 加密往返一致性
    // For any arbitrary string and valid AES key, encrypt then decrypt
    // should return the original string.
    // **Validates: Requirements 13.7, 17.5**
    // ========================================================================

    @Property(tries = 500)
    void property39_encryptThenDecryptReturnsOriginal_aes128(
            @ForAll("arbitraryStrings") String plainText) {
        byte[] key = generateKey(16);

        String encrypted = AesEncryptionUtil.encrypt(plainText, key);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Property(tries = 200)
    void property39_encryptThenDecryptReturnsOriginal_aes192(
            @ForAll("arbitraryStrings") String plainText) {
        byte[] key = generateKey(24);

        String encrypted = AesEncryptionUtil.encrypt(plainText, key);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Property(tries = 500)
    void property39_encryptThenDecryptReturnsOriginal_aes256(
            @ForAll("arbitraryStrings") String plainText) {
        byte[] key = generateKey(32);

        String encrypted = AesEncryptionUtil.encrypt(plainText, key);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Property(tries = 200)
    void property39_samePlaintextProducesDifferentCiphertexts(
            @ForAll("arbitraryStrings") String plainText) {
        byte[] key = generateKey(16);

        String encrypted1 = AesEncryptionUtil.encrypt(plainText, key);
        String encrypted2 = AesEncryptionUtil.encrypt(plainText, key);

        // Due to random IV in GCM mode, ciphertexts should differ
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Property(tries = 100)
    void property39_nullInputReturnsNull(@ForAll("keySizes") int keySize) {
        byte[] key = generateKey(keySize);

        assertThat(AesEncryptionUtil.encrypt(null, key)).isNull();
        assertThat(AesEncryptionUtil.decrypt(null, key)).isNull();
    }

    @Property(tries = 100)
    void property39_decryptWithWrongKeyThrows(
            @ForAll("arbitraryNonEmptyStrings") String plainText) {
        byte[] key1 = generateKey(16);
        byte[] key2 = generateKey(16);

        String encrypted = AesEncryptionUtil.encrypt(plainText, key1);

        assertThatThrownBy(() -> AesEncryptionUtil.decrypt(encrypted, key2))
                .isInstanceOf(BusinessException.class);
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> arbitraryStrings() {
        // Use multiple valid Unicode ranges, excluding surrogates (U+D800–U+DFFF)
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(500)
                .withCharRange('\u0020', '\u007E')   // ASCII printable
                .withCharRange('\u00A0', '\u04FF')   // Latin extended, Cyrillic
                .withCharRange('\u4E00', '\u9FFF')   // CJK Unified Ideographs (Chinese)
                .withCharRange('\u3040', '\u30FF');   // Hiragana + Katakana (Japanese)
    }

    @Provide
    Arbitrary<String> arbitraryNonEmptyStrings() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200);
    }

    @Provide
    Arbitrary<Integer> keySizes() {
        return Arbitraries.of(16, 24, 32);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private static byte[] generateKey(int length) {
        byte[] key = new byte[length];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
