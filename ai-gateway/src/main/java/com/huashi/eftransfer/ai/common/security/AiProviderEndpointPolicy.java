package com.huashi.eftransfer.ai.common.security;

import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

@Component
public class AiProviderEndpointPolicy {

    private final boolean allowPrivateNetworks;
    private final boolean requireHttps;

    public AiProviderEndpointPolicy(
            @Value("${ai.provider-endpoint-policy.allow-private-networks:true}") boolean allowPrivateNetworks,
            @Value("${ai.provider-endpoint-policy.require-https:false}") boolean requireHttps
    ) {
        this.allowPrivateNetworks = allowPrivateNetworks;
        this.requireHttps = requireHttps;
    }

    public void verify(String providerName, AiOpsProviderDefinition definition) {
        if (definition == null) {
            return;
        }
        verifyUrl(providerName, "chat", definition.chat() == null ? null : definition.chat().baseUrl());
        verifyUrl(providerName, "embedding", definition.embedding() == null ? null : definition.embedding().baseUrl());
        verifyUrl(providerName, "rerank", definition.rerank() == null ? null : definition.rerank().baseUrl());
    }

    private void verifyUrl(String providerName, String capability, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        URI uri = URI.create(value.trim());
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw invalid(providerName, capability, "must use http or https");
        }
        if (requireHttps && !"https".equalsIgnoreCase(scheme)) {
            throw invalid(providerName, capability, "must use https in the current environment");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw invalid(providerName, capability, "must use an absolute URL with a host");
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw invalid(providerName, capability, "must not contain user information, query parameters, or fragments");
        }
        if (allowPrivateNetworks) {
            return;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if ("localhost".equals(host) || host.endsWith(".localhost")) {
            throw invalid(providerName, capability, "must not target localhost in the current environment");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw invalid(providerName, capability, "host did not resolve to an address");
            }
            for (InetAddress address : addresses) {
                if (isNonPublic(address)) {
                    throw invalid(providerName, capability, "must not target a private, local, or reserved address");
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid(providerName, capability, "host could not be resolved");
        }
    }

    private boolean isNonPublic(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0
                    || first >= 224
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 198 && (second == 18 || second == 19));
        }
        if (address instanceof Inet6Address && bytes.length == 16) {
            int first = Byte.toUnsignedInt(bytes[0]);
            return (first & 0xfe) == 0xfc;
        }
        return false;
    }

    private IllegalArgumentException invalid(String providerName, String capability, String reason) {
        return new IllegalArgumentException(
                "provider " + providerName + " " + capability + " baseUrl " + reason
        );
    }
}
