package com.huashi.eftransfer.ai.common.security;

import com.huashi.eftransfer.ai.common.config.InternalApiProperties;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiTokenAuthenticator {

    private final InternalApiProperties properties;

    public InternalApiTokenAuthenticator(InternalApiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateConfiguration() {
        if (properties.isEnabled() && !StringUtils.hasText(properties.getToken())) {
            throw new IllegalStateException("platform.internal-api.token must be configured when internal API protection is enabled");
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
