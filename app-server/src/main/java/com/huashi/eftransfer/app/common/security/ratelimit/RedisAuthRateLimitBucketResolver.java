package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.AuthRateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

public class RedisAuthRateLimitBucketResolver implements AuthRateLimitBucketResolver {

    private final ProxyManager<String> proxyManager;

    public RedisAuthRateLimitBucketResolver(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public Bucket resolve(String key, AuthRateLimitProperties.RateLimitWindow limit) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(limit.getLimit(), limit.getWindow()))
                .build();
        return proxyManager.getProxy(key, () -> configuration);
    }
}
