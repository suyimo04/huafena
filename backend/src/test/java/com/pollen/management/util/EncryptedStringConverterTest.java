package com.pollen.management.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Test
    void convertToDatabaseColumn_shouldReturnNullForNullInput() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_shouldReturnNullForNullInput() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
