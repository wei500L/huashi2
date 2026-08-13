package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.InternalApiProperties;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.security.SecretPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiTokenAuthenticator {

    private final InternalApiProperties properties;
    private final Environment environment;

    public InternalApiTokenAuthenticator(InternalApiProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validateConfiguration() {
        boolean relaxed = SecretPolicy.allowsInsecureDefaults(environment.getActiveProfiles());
        if (!properties.isEnabled()) {
            if (!relaxed) {
                throw new IllegalStateException("platform.internal-api.enabled must be true outside local/test profiles");
            }
            return;
        }
        if (!StringUtils.hasText(properties.getToken())) {
            throw new IllegalStateException("platform.internal-api.token must be configured when internal API protection is enabled");
        }
        if (!relaxed) {
            SecretPolicy.validateHighEntropy(properties.getToken(), "platform.internal-api.token");
        }
    }

    public void authenticate(String token) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(token) || !constantTimeEquals(properties.getToken(), token)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Invalid internal API token", 403);
        }
    }

    private boolean constantTimeEquals(String expectedToken, String actualToken) {
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
