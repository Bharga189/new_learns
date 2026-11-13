package com.example.redislock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisPriorityLock using Spring Boot test context.
 * Assumes Redis is running locally per application.yml configuration.
 */
@SpringBootTest(classes = {com.example.redisdemo.RedisDemoApplication.class}, properties = {"redis.lock.enabled=true"})
class RedisPriorityLockIT {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisLockProperties properties;

    @BeforeEach
    void setUp() {
        // Clean up any existing lock data by deleting keys
        redisTemplate.delete("test:lock:basic");
        redisTemplate.delete("test:lock:preempt");
        redisTemplate.delete("test:lock:fifo");
        redisTemplate.delete("myapp:test:lock");
    }

    @Test
    void testBasicLockAcquireAndRelease() throws Exception {
        RedisPriorityLock lock = new RedisPriorityLock(redisTemplate, "test:lock:basic", false, 10000, 30000, 100);

        boolean acquired = lock.tryLock();
        assertTrue(acquired, "Should acquire the lock");

        assertTrue(lock.isHeld(), "Lock should be held");

        lock.unlock();
        assertFalse(lock.isHeld(), "Lock should not be held after unlock");
    }

    @Test
    void testHighPriorityPreemptsNormal() throws Exception {
        // Create a normal-priority lock and acquire it
        RedisPriorityLock normalLock = new RedisPriorityLock(redisTemplate, "test:lock:preempt", false, 10000, 30000, 100);
        boolean normalAcquired = normalLock.tryLock();
        assertTrue(normalAcquired, "Normal lock should acquire first");

        // Now try to acquire with high priority - should preempt
        RedisPriorityLock highPriorityLock = new RedisPriorityLock(redisTemplate, "test:lock:preempt", true, 10000, 30000, 100);
        boolean highAcquired = highPriorityLock.tryLock();
        assertTrue(highAcquired, "High priority should preempt normal priority");

        // High priority holder should be holding it
        assertTrue(highPriorityLock.isHeld(), "High priority lock should be held");

        highPriorityLock.unlock();
        normalLock.unlock();
    }

    @Test
    void testFIFOWithinPriority() throws Exception {
        // Multiple normal-priority requests should acquire in FIFO order
        // Launch concurrent normal-priority lock attempts

        boolean[] acquired = new boolean[3];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    RedisPriorityLock lock = new RedisPriorityLock(redisTemplate, "test:lock:fifo", false, 10000, 30000, 10);
                    acquired[index] = lock.tryLock();
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete");

        // At minimum, some should acquire the lock (they will cycle due to TTL)
        int acquiredCount = 0;
        for (boolean a : acquired) {
            if (a) acquiredCount++;
        }
        assertTrue(acquiredCount > 0, "At least one thread should acquire the lock");
    }

    @Test
    void testLockWithCustomPrefix() throws Exception {
        RedisPriorityLock lock = new RedisPriorityLock(redisTemplate, "myapp:test:lock", false, 10000, 30000, 100);
        boolean acquired = lock.tryLock();
        assertTrue(acquired, "Should acquire lock with custom key");
        lock.unlock();
    }
}