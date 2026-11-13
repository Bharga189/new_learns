package com.example.redisdemo.config;

import com.example.redislock.RedisLockProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration bean for Redis locking properties.
 * Ensures RedisLockProperties is available as a Spring bean.
 */
@Configuration
public class RedisLockConfig {

    @Value("${redis.lock.enabled:true}")
    private boolean enabled;

    @Value("${redis.lock.prefix:lock}")
    private String prefix;

    @Value("${redis.lock.wait-time:10000}")
    private long waitTime;

    @Value("${redis.lock.lease-time:30000}")
    private long leaseTime;

    @Value("${redis.lock.retry-interval:100}")
    private long retryInterval;

    @Bean
    @Primary
    public RedisLockProperties redisLockProperties() {
        RedisLockProperties properties = new RedisLockProperties();
        properties.setEnabled(enabled);
        properties.setPrefix(prefix);
        properties.setWaitTime(waitTime);
        properties.setLeaseTime(leaseTime);
        properties.setRetryInterval(retryInterval);
        return properties;
    }
}