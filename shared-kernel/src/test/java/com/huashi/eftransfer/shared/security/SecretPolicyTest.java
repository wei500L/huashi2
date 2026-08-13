package com.huashi.eftransfer.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretPolicyTest {

    @Test
    void shouldTreatLocalAndTestAsRelaxedProfiles() {
        assertTrue(SecretPolicy.allowsInsecureDefaults("local"));
        assertTrue(SecretPolicy.allowsInsecureDefaults("test"));
        assertFalse(SecretPolicy.allowsInsecureDefaults("prod"));
        assertFalse(SecretPolicy.allowsInsecureDefaults("dev"));
        assertFalse(SecretPolicy.allowsInsecureDefaults());
    }

    @Test
    void shouldRejectKnownInRepoDefaultsAndPlaceholders() {
        assertThrows(IllegalStateException.class, () ->
                SecretPolicy.validateHighEntropy(
                        "local-only-change-this-participant-code-secret",
                        "hmac"));
        assertThrows(IllegalStateException.class, () ->
                SecretPolicy.validateHighEntropy(
                        "local-only-change-this-profile-key",
                        "profile"));
        assertThrows(IllegalStateException.class, () ->
                SecretPolicy.validateHighEntropy(
                        "replace-with-shared-internal-token",
                        "internal"));
        assertThrows(IllegalStateException.class, () ->
                SecretPolicy.validateHighEntropy(
                        "replace-me-with-a-32-byte-minimum-secret-key",
                        "jwt"));
    }

    @Test
    void shouldAcceptHighEntropySecret() {
        assertDoesNotThrow(() ->
                SecretPolicy.validateHighEntropy("x7Pq2Lk9Vd4Nc8Rs1Tf6Yh3Jm5Bw0QeZ", "jwt"));
    }
}
