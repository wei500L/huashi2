package com.huashi.eftransfer.app.modules.assessment.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class AssessmentParticipantCodeCodec {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final SecureRandom secureRandom;

    @Autowired
    public AssessmentParticipantCodeCodec(
            @Value("${app.assessment.public-delivery.hmac-secret}") String secret
    ) {
        this(secret, new SecureRandom());
    }

    public AssessmentParticipantCodeCodec(String secret, SecureRandom secureRandom) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Participant-code HMAC secret must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.secureRandom = secureRandom == null ? new SecureRandom() : secureRandom;
    }

    public String generate() {
        char[] result = new char[14];
        for (int index = 0; index < result.length; index++) {
            if (index == 4 || index == 9) {
                result[index] = '-';
            } else {
                result[index] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
            }
        }
        return new String(result);
    }

    public String digest(String rawCode) {
        return digestOpaque(normalize(rawCode));
    }

    public String digestOpaque(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value to digest is required");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute participant-code digest", exception);
        }
    }

    public boolean matches(String rawCode, String expectedDigest) {
        if (expectedDigest == null || expectedDigest.length() != 64) {
            return false;
        }
        try {
            byte[] actual = HexFormat.of().parseHex(digest(rawCode));
            byte[] expected = HexFormat.of().parseHex(expectedDigest);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public String normalize(String rawCode) {
        if (rawCode == null) {
            throw new IllegalArgumentException("Participant code is required");
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$")) {
            throw new IllegalArgumentException("Participant code must use XXXX-XXXX-XXXX format");
        }
        return normalized;
    }
}
