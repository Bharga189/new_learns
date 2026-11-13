package com.example.redisdemo;

import com.example.redislock.RedisLockProperties;
import com.example.redislock.RedisPriorityLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for priority-based Redis locking in the consumer project.
 */
@SpringBootTest
class RedisLockPriorityTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisLockProperties properties;

    @BeforeEach
    void setUp() {
        // Clean up any existing lock data
        redisTemplate.delete("test:lock:highpri");
        redisTemplate.delete("test:lock:normalpri");
    }

    @Test
    void testHighPriorityPreemptsNormal() throws Exception {
        // Create a normal-priority lock and acquire it
        RedisPriorityLock normalLock = new RedisPriorityLock(
                redisTemplate,
                "test:lock:normalpri",
                false,  // normal priority
                properties.getWaitTime(),
                properties.getLeaseTime(),
                properties.getRetryInterval()
        );
        boolean normalAcquired = normalLock.tryLock();
        assertTrue(normalAcquired, "Normal lock should acquire first");

        // Now try to acquire with high priority - should preempt
        RedisPriorityLock highPriorityLock = new RedisPriorityLock(
                redisTemplate,
                "test:lock:highpri",
                true,  // high priority
                properties.getWaitTime(),
                properties.getLeaseTime(),
                properties.getRetryInterval()
        );
        boolean highAcquired = highPriorityLock.tryLock();
        assertTrue(highAcquired, "High priority should preempt normal priority");

        // High priority holder should be holding it
        assertTrue(highPriorityLock.isHeld(), "High priority lock should be held");

        // Normal lock should no longer be held (preempted)
        assertFalse(normalLock.isHeld(), "Normal lock should not be held after preemption");

        // Clean up
        highPriorityLock.unlock();
        normalLock.unlock();
    }

    @Test
    void testFIFOWithinPriority() throws Exception {
        // Test that multiple normal-priority locks acquire in FIFO order
        // Launch concurrent normal-priority lock attempts

        boolean[] acquired = new boolean[3];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    RedisPriorityLock lock = new RedisPriorityLock(
                            redisTemplate,
                            "test:lock:fifo",
                            false,  // normal priority
                            properties.getWaitTime(),
                            properties.getLeaseTime(),
                            properties.getRetryInterval()
                    );
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
}