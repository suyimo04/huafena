package com.pollen.management.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter：透明加解密 Integer 字段（迷你币金额）
 * 数据库存储为 AES 加密的 Base64 字符串，Java 端使用 Integer
 */
@Converter
public class EncryptedIntegerConverter implements AttributeConverter<Integer, String> {

    @Override
    public String convertToDatabaseColumn(Integer attribute) {
        if (attribute == null) {
            return null;
        }
        return AesEncryptionUtil.encrypt(attribute.toString());
    }

    @Override
    public Integer convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String decrypted = AesEncryptionUtil.decrypt(dbData);
        return Integer.parseInt(decrypted);
    }
}
