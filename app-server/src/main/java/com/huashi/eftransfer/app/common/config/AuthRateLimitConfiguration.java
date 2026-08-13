package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.security.ratelimit.LocalAuthRateLimitBucketResolver;
import com.huashi.eftransfer.app.common.security.ratelimit.RateLimitBucketResolver;
import com.huashi.eftransfer.app.common.security.ratelimit.RedisAuthRateLimitBucketResolver;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.RedissonKeys;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({
        AuthRateLimitProperties.class,
        AiRateLimitProperties.class,
        AssessmentRateLimitProperties.class
})
public class AuthRateLimitConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public RateLimitBucketResolver redisAuthRateLimitBucketResolver(RedissonClient redissonClient) {
        CommandAsyncExecutor commandExecutor = ((RedissonKeys) redissonClient.getKeys()).getCommandExecutor();
        ProxyManager<String> proxyManager = RedissonBasedProxyManager.builderFor(commandExecutor)
                .withKeyMapper(Mapper.STRING)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
                .build();
        return new RedisAuthRateLimitBucketResolver(proxyManager);
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitBucketResolver.class)
    public RateLimitBucketResolver localAuthRateLimitBucketResolver() {
        return new LocalAuthRateLimitBucketResolver();
    }
}
