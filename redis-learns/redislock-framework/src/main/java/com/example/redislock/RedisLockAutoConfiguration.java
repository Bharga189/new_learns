package com.example.redislock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.annotation.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@EnableConfigurationProperties(RedisLockProperties.class)
public class RedisLockAutoConfiguration {

    @Bean
    public RedisLockManager redisLockManager(StringRedisTemplate stringRedisTemplate,
                                             RedisLockProperties properties) {
        return new RedisLockManager(stringRedisTemplate, properties);
    }

    @Bean
    public RedisLockAspect redisLockAspect(RedisLockManager manager, RedisLockProperties properties) {
        return new RedisLockAspect(manager, properties);
    }
}