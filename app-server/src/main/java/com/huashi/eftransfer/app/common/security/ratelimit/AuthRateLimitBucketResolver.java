package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.AuthRateLimitProperties;
import io.github.bucket4j.Bucket;

public interface AuthRateLimitBucketResolver {

    Bucket resolve(String key, AuthRateLimitProperties.RateLimitWindow limit);
}
