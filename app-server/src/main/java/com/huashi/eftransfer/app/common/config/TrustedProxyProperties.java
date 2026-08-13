package com.huashi.eftransfer.app.common.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security.trusted-proxy")
public class TrustedProxyProperties {

    /**
     * CIDR blocks whose socket address may present {@code X-Forwarded-For} /
     * {@code X-Real-IP}. Direct clients outside this list are keyed by
     * {@code remoteAddr} so they cannot spoof rate-limit or audit IPs.
     */
    @NotEmpty
    private List<String> cidrs = new ArrayList<>(List.of(
            "127.0.0.0/8",
            "::1/128",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "fc00::/7"
    ));

    public List<String> getCidrs() {
        return cidrs;
    }

    public void setCidrs(List<String> cidrs) {
        this.cidrs = cidrs == null ? new ArrayList<>() : new ArrayList<>(cidrs);
    }
}
