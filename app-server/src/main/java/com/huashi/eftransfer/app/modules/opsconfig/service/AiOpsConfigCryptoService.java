package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.config.AiOpsConfigProperties;
import com.huashi.eftransfer.app.common.config.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AiOpsConfigCryptoService {

    private static final String PREFIX = "v1:";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec secretKeySpec;

    public AiOpsConfigCryptoService(AiOpsConfigProperties properties, JwtProperties jwtProperties) {
        String secret = StringUtils.hasText(properties.getEncryptionSecret())
                ? properties.getEncryptionSecret()
                : jwtProperties.getSecret();
        this.secretKeySpec = new SecretKeySpec(sha256(secret), "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt AI ops config", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (!cipherText.startsWith(PREFIX)) {
            throw new IllegalStateException("Unsupported AI ops config cipher payload");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(cipherText.substring(PREFIX.length()));
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt AI ops config", ex);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize AI ops config encryption key", ex);
        }
    }
}
