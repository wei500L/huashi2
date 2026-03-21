package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.AuthRateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalAuthRateLimitBucketResolver implements AuthRateLimitBucketResolver {

    private final Map<RateLimitKey, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Bucket resolve(String key, AuthRateLimitProperties.RateLimitWindow limit) {
        RateLimitKey rateLimitKey = new RateLimitKey(key, limit.getLimit(), limit.getWindow());
        return buckets.computeIfAbsent(
                rateLimitKey,
                ignored -> Bucket.builder().addLimit(Bandwidth.simple(limit.getLimit(), limit.getWindow())).build()
        );
    }

    private record RateLimitKey(String key, long limit, Duration window) {
    }
}
