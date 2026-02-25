package com.pollen.management.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加密工具类
 * 密钥通过环境变量 ENCRYPTION_KEY 配置（Base64 编码的 16/24/32 字节密钥）
 */
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENV_KEY_NAME = "ENCRYPTION_KEY";

    private AesEncryptionUtil() {
    }

    /**
     * 加密明文字符串
     * @param plainText 明文
     * @return Base64 编码的密文（IV + 密文拼接）
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] keyBytes = getKeyBytes();
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(500, "加密失败: " + e.getMessage());
        }
    }

    /**
     * 解密密文字符串
     * @param cipherText Base64 编码的密文
     * @return 解密后的明文
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] keyBytes = getKeyBytes();
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(500, "解密失败: " + e.getMessage());
        }
    }

    /**
     * 使用指定密钥加密（用于测试和自定义场景）
     */
    public static String encrypt(String plainText, byte[] keyBytes) {
        if (plainText == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(500, "加密失败: " + e.getMessage());
        }
    }

    /**
     * 使用指定密钥解密（用于测试和自定义场景）
     */
    public static String decrypt(String cipherText, byte[] keyBytes) {
        if (cipherText == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(500, "解密失败: " + e.getMessage());
        }
    }

    private static byte[] getKeyBytes() {
        String envKey = System.getenv(ENV_KEY_NAME);
        if (envKey == null || envKey.isBlank()) {
            throw new BusinessException(500, "环境变量 " + ENV_KEY_NAME + " 未配置");
        }
        byte[] keyBytes = Base64.getDecoder().decode(envKey);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new BusinessException(500, "ENCRYPTION_KEY 长度无效，需要 16/24/32 字节");
        }
        return keyBytes;
    }
}
