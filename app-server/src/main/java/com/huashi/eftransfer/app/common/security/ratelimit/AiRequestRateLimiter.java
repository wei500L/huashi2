package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.AiRateLimitProperties;
import com.huashi.eftransfer.app.common.config.RateLimitWindow;
import com.huashi.eftransfer.app.common.security.ClientRequestContextResolver;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiRequestRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiRequestRateLimiter.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private final AiRateLimitProperties properties;
    private final RateLimitBucketResolver bucketResolver;

    public AiRequestRateLimiter(AiRateLimitProperties properties, RateLimitBucketResolver bucketResolver) {
        this.properties = properties;
        this.bucketResolver = bucketResolver;
    }

    public void check(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return;
        }
        consume(
                "ai:rl:submit:ip:" + ClientRequestContextResolver.resolveIpAddress(request),
                properties.getIp(),
                "ip"
        );
        Long userId = SecurityUtils.getCurrentUserId().orElse(null);
        if (userId != null) {
            consume("ai:rl:submit:user:" + userId, properties.getUser(), "user");
        }
    }

    private void consume(String key, RateLimitWindow limit, String dimension) {
        if (bucketResolver.resolve(key, limit).tryConsume(1)) {
            return;
        }
        log.warn("event=ai_rate_limited dimension={} key={}", dimension, key);
        throw new BusinessException(ResultCode.RATE_LIMITED, "Too many AI requests", TOO_MANY_REQUESTS);
    }
}
