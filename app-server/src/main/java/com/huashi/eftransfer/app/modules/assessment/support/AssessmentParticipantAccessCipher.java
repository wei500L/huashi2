package com.huashi.eftransfer.app.modules.assessment.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AssessmentParticipantAccessCipher {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final byte[] AAD = "assessment-participant-ip-v1".getBytes(StandardCharsets.UTF_8);
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final String keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public AssessmentParticipantAccessCipher(
            @Value("${app.assessment.sensitive-profile-key}") String secret,
            @Value("${app.assessment.sensitive-profile-key-version:v1}") String keyVersion
    ) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize participant access encryption", exception);
        }
        this.keyVersion = keyVersion;
    }

    public EncryptedValue encrypt(String value) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv), keyVersion);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt participant access data", exception);
        }
    }

    public String decrypt(String ciphertext, String iv, String storedKeyVersion) {
        if (!keyVersion.equals(storedKeyVersion)) {
            throw new IllegalStateException("Unsupported participant access key version");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(iv)));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decrypt participant access data", exception);
        }
    }

    public record EncryptedValue(String ciphertext, String iv, String keyVersion) { }
}
