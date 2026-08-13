package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.RateLimitWindow;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

public class RedisAuthRateLimitBucketResolver implements RateLimitBucketResolver {

    private final ProxyManager<String> proxyManager;

    public RedisAuthRateLimitBucketResolver(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public Bucket resolve(String key, RateLimitWindow limit) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(limit.getLimit(), limit.getWindow()))
                .build();
        return proxyManager.getProxy(key, () -> configuration);
    }
}
