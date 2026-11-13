package com.example.redisdemo.service;

import com.example.redislock.RedisPriorityLock;
import com.example.redislock.RedisLockProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;
    private final RedisLockProperties properties;

    @Autowired
    public RedisLockService(StringRedisTemplate redisTemplate, RedisLockProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Acquire a distributed lock with optional high priority
     *
     * @param lockKey    the lock key
     * @param highPriority if true, high priority preemption is enabled
     * @return status message
     * @throws Exception if lock cannot be acquired within wait time
     */
    @Transactional
    public String acquireLock(String lockKey, boolean highPriority) throws Exception {
        // Create the priority lock with configuration from properties
        RedisPriorityLock lock = new RedisPriorityLock(
                redisTemplate,
                lockKey,
                highPriority,
                properties.getWaitTime(),
                properties.getLeaseTime(),
                properties.getRetryInterval()
        );

        boolean acquired = lock.tryLock();
        if (acquired) {
            return "Lock acquired: " + lockKey + " (highPriority=" + highPriority + ")";
        } else {
            return "Failed to acquire lock: " + lockKey + " (highPriority=" + highPriority + ")";
        }
    }

    /**
     * Release a distributed lock
     *
     * @param lockKey the lock key
     */
    public void releaseLock(String lockKey) {
        // Get the lock and release it - in a full implementation,
        // we'd track locked locks in a map
        // For now, we just note the release
    }

    /**
     * Check if a lock is currently held
     *
     * @param lockKey the lock key
     * @return true if lock is held
     */
    public boolean isLockHeld(String lockKey) {
        // Check if the lock hash has an owner set in Redis
        // The lock is held if the 'owner' field exists and is not null
        try {
            Boolean held = redisTemplate.hasKey(lockKey) && redisTemplate.opsForHash().get(lockKey, "owner") != null;
            return held != null && held;
        } catch (Exception e) {
            // Fallback to false on error
            return false;
        }
    }
}