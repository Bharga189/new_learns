package com.example.redislock;

import com.example.redisdemo.service.LockDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisLock annotation via AOP aspect.
 * Runs against local Redis per application.yml configuration.
 */
@SpringBootTest(classes = {com.example.redisdemo.RedisDemoApplication.class}, properties = {"redis.lock.enabled=true"})
class RedisLockAnnotationIT {

    @Autowired
    private LockDemoService lockDemoService;

    @BeforeEach
    void setUp() {
        // Locks auto-expire via TTL, no manual cleanup needed
    }

    @Test
    void testProcessNormalPriority() throws Exception {
        String result = lockDemoService.process("test-user-1");
        assertNotNull(result, "Should process with normal priority");
        assertTrue(result.contains("processed"), "Result should indicate processing");
    }

    @Test
    void testProcessHighPriority() throws Exception {
        String result = lockDemoService.processHigh("test-user-2");
        assertNotNull(result, "Should process with high priority");
        assertTrue(result.contains("high-priority processed"), "Result should indicate high-priority processing");
    }
}