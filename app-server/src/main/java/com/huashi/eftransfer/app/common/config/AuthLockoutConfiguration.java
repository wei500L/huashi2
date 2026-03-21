package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.security.lockout.AuthLockoutStore;
import com.huashi.eftransfer.app.common.security.lockout.LocalAuthLockoutStore;
import com.huashi.eftransfer.app.common.security.lockout.RedisAuthLockoutStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(AuthLockoutProperties.class)
public class AuthLockoutConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(AuthLockoutStore.class)
    public AuthLockoutStore redisAuthLockoutStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisAuthLockoutStore(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(AuthLockoutStore.class)
    public AuthLockoutStore localAuthLockoutStore() {
        return new LocalAuthLockoutStore();
    }
}
