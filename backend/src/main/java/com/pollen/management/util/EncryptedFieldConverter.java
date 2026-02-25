package com.pollen.management.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * JPA AttributeConverter：透明加解密 BigDecimal 字段（薪资数据）
 * 数据库存储为 AES 加密的 Base64 字符串，Java 端使用 BigDecimal
 */
@Converter
public class EncryptedFieldConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) {
            return null;
        }
        return AesEncryptionUtil.encrypt(attribute.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String decrypted = AesEncryptionUtil.decrypt(dbData);
        return new BigDecimal(decrypted);
    }
}
