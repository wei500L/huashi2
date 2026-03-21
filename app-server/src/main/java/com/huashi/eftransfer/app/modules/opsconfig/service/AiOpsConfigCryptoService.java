package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.config.AiOpsConfigProperties;
import com.huashi.eftransfer.app.common.config.JwtProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class AiOpsConfigCryptoService {

    private static final String PREFIX = "v1:";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec primarySecretKeySpec;
    private final SecretKeySpec legacySecretKeySpec;
    private final boolean localOrTestProfile;

    public AiOpsConfigCryptoService(AiOpsConfigProperties properties, JwtProperties jwtProperties, Environment environment) {
        this.localOrTestProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> "local".equals(profile) || "test".equals(profile));
        String primarySecret = properties.getEncryptionSecret();
        String legacySecret = jwtProperties.resolveLegacySecret();

        if (!StringUtils.hasText(primarySecret) && !localOrTestProfile) {
            throw new IllegalStateException("APP_OPS_CONFIG_ENCRYPTION_SECRET must be configured outside local/test profiles");
        }

        this.primarySecretKeySpec = StringUtils.hasText(primarySecret)
                ? new SecretKeySpec(sha256(primarySecret), "AES")
                : null;
        this.legacySecretKeySpec = StringUtils.hasText(legacySecret)
                ? new SecretKeySpec(sha256(legacySecret), "AES")
                : null;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = encryptionKey();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
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
            return decryptWithFallback(cipherText);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt AI ops config", ex);
        }
    }

    private String decryptWithFallback(String cipherText) throws Exception {
        if (primarySecretKeySpec != null) {
            try {
                return decrypt(cipherText, primarySecretKeySpec);
            } catch (Exception ignored) {
                if (legacySecretKeySpec == null || isSameKey(primarySecretKeySpec, legacySecretKeySpec)) {
                    throw ignored;
                }
            }
        }
        if (legacySecretKeySpec != null) {
            return decrypt(cipherText, legacySecretKeySpec);
        }
        throw new IllegalStateException("No AI ops config decryption key is configured");
    }

    private String decrypt(String cipherText, SecretKeySpec keySpec) throws Exception {
        byte[] payload = Base64.getUrlDecoder().decode(cipherText.substring(PREFIX.length()));
        byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_LENGTH);
        byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKeySpec encryptionKey() {
        if (primarySecretKeySpec != null) {
            return primarySecretKeySpec;
        }
        if (localOrTestProfile && legacySecretKeySpec != null) {
            return legacySecretKeySpec;
        }
        throw new IllegalStateException("APP_OPS_CONFIG_ENCRYPTION_SECRET must be configured for AI ops config encryption");
    }

    private boolean isSameKey(SecretKeySpec left, SecretKeySpec right) {
        return java.util.Arrays.equals(left.getEncoded(), right.getEncoded());
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize AI ops config encryption key", ex);
        }
    }
}
