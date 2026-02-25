package com.pollen.management.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter：透明加解密 String 字段（如 SMTP 密码）
 * 数据库存储为 AES 加密的 Base64 字符串，Java 端使用明文 String
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return AesEncryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return AesEncryptionUtil.decrypt(dbData);
    }
}
