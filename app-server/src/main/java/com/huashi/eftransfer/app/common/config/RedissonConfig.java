package com.huashi.eftransfer.app.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "app.redis.redisson-enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.ssl:false}") boolean ssl
    ) {
        Config config = new Config();
        String prefix = ssl ? "rediss://" : "redis://";
        String address = prefix + host + ":" + port;

        var singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);

        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}
