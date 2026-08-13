package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.security.ratelimit.AiRequestRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AiRateLimitWebConfiguration implements WebMvcConfigurer {

    private final AiRequestRateLimitInterceptor aiRequestRateLimitInterceptor;

    public AiRateLimitWebConfiguration(AiRequestRateLimitInterceptor aiRequestRateLimitInterceptor) {
        this.aiRequestRateLimitInterceptor = aiRequestRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(aiRequestRateLimitInterceptor)
                .addPathPatterns("/api/ai/**", "/api/teacher/intervention-suggest");
    }
}
