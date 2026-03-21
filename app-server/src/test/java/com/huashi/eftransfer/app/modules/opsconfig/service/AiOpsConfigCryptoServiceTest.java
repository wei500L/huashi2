package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.config.AiOpsConfigProperties;
import com.huashi.eftransfer.app.common.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiOpsConfigCryptoServiceTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OPS_SECRET = "fedcba9876543210fedcba9876543210";

    @Test
    void shouldEncryptAndDecryptWithDedicatedOpsSecret() {
        AiOpsConfigCryptoService cryptoService = new AiOpsConfigCryptoService(
                opsProperties(OPS_SECRET),
                jwtProperties(JWT_SECRET),
                environment("test")
        );

        String cipherText = cryptoService.encrypt("sensitive-value");

        assertEquals("sensitive-value", cryptoService.decrypt(cipherText));
    }

    @Test
    void shouldDecryptLegacyCiphertextUsingJwtFallbackAndReencryptWithOpsSecret() {
        AiOpsConfigCryptoService legacyCryptoService = new AiOpsConfigCryptoService(
                opsProperties(""),
                jwtProperties(JWT_SECRET),
                environment("local")
        );
        String legacyCipherText = legacyCryptoService.encrypt("chat-secret-001");

        AiOpsConfigCryptoService migratedCryptoService = new AiOpsConfigCryptoService(
                opsProperties(OPS_SECRET),
                jwtProperties(JWT_SECRET),
                environment("prod")
        );

        String decrypted = migratedCryptoService.decrypt(legacyCipherText);
        String reencrypted = migratedCryptoService.encrypt(decrypted);

        assertEquals("chat-secret-001", decrypted);
        assertNotEquals(legacyCipherText, reencrypted);
        assertEquals("chat-secret-001", migratedCryptoService.decrypt(reencrypted));
    }

    @Test
    void shouldRequireDedicatedOpsSecretOutsideLocalOrTest() {
        assertThrows(IllegalStateException.class, () -> new AiOpsConfigCryptoService(
                opsProperties(""),
                jwtProperties(JWT_SECRET),
                environment("prod")
        ));
    }

    private AiOpsConfigProperties opsProperties(String secret) {
        AiOpsConfigProperties properties = new AiOpsConfigProperties();
        properties.setEncryptionSecret(secret);
        return properties;
    }

    private JwtProperties jwtProperties(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        return properties;
    }

    private MockEnvironment environment(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return environment;
    }
}
