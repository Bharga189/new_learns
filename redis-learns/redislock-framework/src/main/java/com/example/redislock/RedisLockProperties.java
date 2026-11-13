package com.example.redislock;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.lock")
public class RedisLockProperties {

    public RedisLockProperties() {
        // No-arg constructor required by Spring Boot ConfigurationProperties
    }

    private boolean enabled = true;

    /** Global key prefix read from application.yml */
    private String prefix = "lock";

    /** Default wait time (ms) to acquire a lock */
    private long waitTime = 10000L;

    /** Default lease time (ms) before a lock auto-expires */
    private long leaseTime = 30000L;

    /** Polling interval (ms) while waiting for a lock */
    private long retryInterval = 100L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public long getWaitTime() { return waitTime; }
    public void setWaitTime(long waitTime) { this.waitTime = waitTime; }

    public long getLeaseTime() { return leaseTime; }
    public void setLeaseTime(long leaseTime) { this.leaseTime = leaseTime; }

    public long getRetryInterval() { return retryInterval; }
    public void setRetryInterval(long retryInterval) { this.retryInterval = retryInterval; }
}