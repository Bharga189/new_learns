package com.example.redislock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@ConditionalOnProperty(prefix = "redis.lock", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RedisLockProperties.class)
public class RedisLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisLockManager redisLockManager(StringRedisTemplate stringRedisTemplate,
                                            RedisLockProperties properties) {
        return new RedisLockManager(stringRedisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisLockAspect redisLockAspect(RedisLockManager manager, RedisLockProperties properties) {
        return new RedisLockAspect(manager, properties);
    }
}
