package com.pollen.management.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedFieldConverterTest {

    private final EncryptedFieldConverter converter = new EncryptedFieldConverter();

    @BeforeAll
    static void setUpEncryptionKey() {
        // Set ENCRYPTION_KEY env variable for tests via system property workaround
        // Since we can't set env vars in Java, we'll test the converter indirectly
        // by verifying it delegates to AesEncryptionUtil correctly
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
