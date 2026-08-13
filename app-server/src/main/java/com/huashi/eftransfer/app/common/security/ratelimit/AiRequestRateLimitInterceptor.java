package com.huashi.eftransfer.app.common.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AiRequestRateLimitInterceptor implements HandlerInterceptor {

    private final AiRequestRateLimiter aiRequestRateLimiter;

    public AiRequestRateLimitInterceptor(AiRequestRateLimiter aiRequestRateLimiter) {
        this.aiRequestRateLimiter = aiRequestRateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        aiRequestRateLimiter.check(request);
        return true;
    }
}
