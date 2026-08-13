package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.RateLimitWindow;
import io.github.bucket4j.Bucket;

public interface RateLimitBucketResolver {

    Bucket resolve(String key, RateLimitWindow limit);
}
