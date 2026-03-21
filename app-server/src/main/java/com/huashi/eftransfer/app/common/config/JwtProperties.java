package com.huashi.eftransfer.app.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private static final String DEFAULT_ACTIVE_KID = "current";

    private String secret = "";

    private String legacySecret = "";

    private String activeKid = DEFAULT_ACTIVE_KID;

    private List<SigningKeyProperties> keys = new ArrayList<>();

    @NotBlank
    private String issuer = "ef-transfer-platform";

    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(30);

    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(7);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getLegacySecret() {
        return legacySecret;
    }

    public void setLegacySecret(String legacySecret) {
        this.legacySecret = legacySecret;
    }

    public String getActiveKid() {
        return activeKid;
    }

    public void setActiveKid(String activeKid) {
        this.activeKid = activeKid;
    }

    public List<SigningKeyProperties> getKeys() {
        return keys;
    }

    public void setKeys(List<SigningKeyProperties> keys) {
        this.keys = keys == null ? new ArrayList<>() : keys;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String resolveLegacySecret() {
        if (StringUtils.hasText(legacySecret)) {
            return legacySecret;
        }
        return secret;
    }

    public String resolveActiveKid() {
        if (StringUtils.hasText(activeKid)) {
            return activeKid;
        }
        List<SigningKeyProperties> configuredKeys = resolveConfiguredKeys();
        if (!configuredKeys.isEmpty()) {
            return configuredKeys.get(0).getKid();
        }
        return DEFAULT_ACTIVE_KID;
    }

    public List<SigningKeyProperties> resolveConfiguredKeys() {
        List<SigningKeyProperties> configured = new ArrayList<>();
        for (SigningKeyProperties key : keys) {
            if (key != null && StringUtils.hasText(key.getKid()) && StringUtils.hasText(key.getSecret())) {
                configured.add(key);
            }
        }
        if (!configured.isEmpty()) {
            return List.copyOf(configured);
        }

        String fallbackSecret = resolveLegacySecret();
        if (StringUtils.hasText(fallbackSecret)) {
            SigningKeyProperties fallback = new SigningKeyProperties();
            fallback.setKid(resolveActiveKidOrDefault());
            fallback.setSecret(fallbackSecret);
            return List.of(fallback);
        }
        return List.of();
    }

    private String resolveActiveKidOrDefault() {
        return StringUtils.hasText(activeKid) ? activeKid : DEFAULT_ACTIVE_KID;
    }

    public static class SigningKeyProperties {

        private String kid = "";

        private String secret = "";

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
