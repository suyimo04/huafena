package com.pollen.management.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedIntegerConverterTest {

    private final EncryptedIntegerConverter converter = new EncryptedIntegerConverter();

    @BeforeAll
    static void setUpEncryptionKey() {
        // AesEncryptionUtil reads from env var ENCRYPTION_KEY
        // For unit tests, we test null handling directly; round-trip tests
        // require the env var to be set (handled in SalaryEncryptionTest)
    }

    @Test
    void convertToDatabaseColumn_shouldReturnNullForNullInput() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_shouldReturnNullForNullInput() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
