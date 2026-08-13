package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.AssessmentRateLimitProperties;
import com.huashi.eftransfer.app.common.config.RateLimitWindow;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicAssessmentRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PublicAssessmentRateLimiter.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private final AssessmentRateLimitProperties properties;
    private final RateLimitBucketResolver bucketResolver;

    public PublicAssessmentRateLimiter(
            AssessmentRateLimitProperties properties,
            RateLimitBucketResolver bucketResolver
    ) {
        this.properties = properties;
        this.bucketResolver = bucketResolver;
    }

    public void checkVerify(String remoteAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        consume(
                "assessment:rl:verify:ip:" + key(remoteAddress),
                properties.getVerify().getIp(),
                "verify"
        );
    }

    public void checkQrEntry(String normalizedIp) {
        if (!properties.isEnabled()) {
            return;
        }
        consume(
                "assessment:rl:qr-entry:ip:" + key(normalizedIp),
                properties.getQrEntry().getIp(),
                "qr-entry"
        );
    }

    private void consume(String bucketKey, RateLimitWindow limit, String action) {
        if (bucketResolver.resolve(bucketKey, limit).tryConsume(1)) {
            return;
        }
        log.warn("event=assessment_rate_limited action={} key={}", action, bucketKey);
        throw new BusinessException(ResultCode.RATE_LIMITED, "Too many participant-code attempts", TOO_MANY_REQUESTS);
    }

    private String key(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
