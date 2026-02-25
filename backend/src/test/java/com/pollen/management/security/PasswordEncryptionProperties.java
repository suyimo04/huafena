package com.pollen.management.security;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 27: 密码 BCrypt 加密存储
 * **Validates: Requirements 13.1**
 *
 * For any 存储的用户密码，密码值应为 BCrypt 编码格式，且不等于明文密码。
 */
class PasswordEncryptionProperties {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Property(tries = 100)
    void encodedPasswordIsNotEqualToPlaintext(@ForAll @StringLength(min = 1, max = 50) String plaintext) {
        String encoded = encoder.encode(plaintext);

        assertThat(encoded).isNotEqualTo(plaintext);
    }

    @Property(tries = 100)
    void encodedPasswordStartsWithBCryptPrefix(@ForAll @StringLength(min = 1, max = 50) String plaintext) {
        String encoded = encoder.encode(plaintext);

        assertThat(encoded).startsWith("$2");
    }

    @Property(tries = 100)
    void encodedPasswordCanBeVerifiedWithMatches(@ForAll @StringLength(min = 1, max = 50) String plaintext) {
        String encoded = encoder.encode(plaintext);

        assertThat(encoder.matches(plaintext, encoded)).isTrue();
    }
}
