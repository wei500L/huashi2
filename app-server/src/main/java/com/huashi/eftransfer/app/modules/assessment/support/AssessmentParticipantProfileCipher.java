package com.huashi.eftransfer.app.modules.assessment.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Component
public class AssessmentParticipantProfileCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final ObjectMapper objectMapper;
    private final SecretKeySpec key;
    private final String keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public AssessmentParticipantProfileCipher(
            ObjectMapper objectMapper,
            @Value("${app.assessment.sensitive-profile-key:local-only-change-this-profile-key}") String secret,
            @Value("${app.assessment.sensitive-profile-key-version:v1}") String keyVersion
    ) {
        this.objectMapper = objectMapper;
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize participant profile encryption", exception);
        }
        this.key = new SecretKeySpec(digest, "AES");
        this.keyVersion = keyVersion;
    }

    public EncryptedProfile encrypt(Map<String, ?> profile) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(objectMapper.writeValueAsString(profile).getBytes(StandardCharsets.UTF_8));
            return new EncryptedProfile(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv),
                    keyVersion
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt participant profile", exception);
        }
    }

    public record EncryptedProfile(String ciphertext, String iv, String keyVersion) { }
}
