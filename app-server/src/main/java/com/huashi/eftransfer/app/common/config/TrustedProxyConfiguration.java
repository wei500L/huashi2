package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.security.ClientRequestContextResolver;
import org.springframework.stereotype.Component;

@Component
public class TrustedProxyConfiguration {

    public TrustedProxyConfiguration(TrustedProxyProperties properties) {
        ClientRequestContextResolver.configureTrustedProxies(properties.getCidrs());
    }
}
