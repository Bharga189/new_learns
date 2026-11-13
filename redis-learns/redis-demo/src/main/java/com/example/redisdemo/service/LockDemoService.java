package com.example.redisdemo.service;

import com.example.redislock.RedisLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LockDemoService {

    private final StringRedisTemplate redis;

    public LockDemoService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @RedisLock(keys = {"#id"}, highPriority = false)
    public String process(String id) throws InterruptedException {
        // critical section
        long before = System.currentTimeMillis();
        TimeUnit.MILLISECONDS.sleep(300);
        redis.opsForValue().increment("counter:" + id);
        long after = System.currentTimeMillis();
        return "processed " + id + " in " + (after - before) + "ms";
    }

    @RedisLock(keys = {"#id"}, highPriority = true)
    public String processHigh(String id) throws InterruptedException {
        long before = System.currentTimeMillis();
        TimeUnit.MILLISECONDS.sleep(100);
        redis.opsForValue().increment("counter:" + id);
        long after = System.currentTimeMillis();
        return "high-priority processed " + id + " in " + (after - before) + "ms";
    }
}
