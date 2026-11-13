package com.example.redisdemo.controller;

import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Redisson LOCKS & SYNCHRONIZATION
 * --------------------------------
 * Distributed locks built on Redis (Redlock algorithm). Work across JVMs.
 * RLock: reentrant mutex. RReadWriteLock: read/write. RSemaphore: permits.
 * RCountDownLatch: distributed countdown. RRateLimiter: rate limiting.
 */
@RestController
@RequestMapping("/api/redisson/locks")
public class RedissonLocksController {

    private final RedissonClient redisson;

    public RedissonLocksController(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @PostMapping("/lock/{name}")
    public String lock(@PathVariable String name, @RequestParam(defaultValue = "10") long leaseSeconds) throws InterruptedException {
        RLock lock = redisson.getLock(name);
        if (lock.tryLock(5, leaseSeconds, TimeUnit.SECONDS)) {      // tryLock with auto-expiry (leaseTime)
            try {
                Thread.sleep(1000);                                 // critical section
                return "acquired and released";
            } finally {
                lock.unlock();
            }
        }
        return "not acquired";
    }

    @PostMapping("/readwrite/{name}/read")
    public String readLock(@PathVariable String name) throws InterruptedException {
        RReadWriteLock rw = redisson.getReadWriteLock(name);
        if (rw.readLock().tryLock(5, 10, TimeUnit.SECONDS)) {
            try { Thread.sleep(500); return "read-locked"; }
            finally { rw.readLock().unlock(); }
        }
        return "not acquired";
    }

    @PostMapping("/readwrite/{name}/write")
    public String writeLock(@PathVariable String name) throws InterruptedException {
        RReadWriteLock rw = redisson.getReadWriteLock(name);
        if (rw.writeLock().tryLock(5, 10, TimeUnit.SECONDS)) {
            try { Thread.sleep(500); return "write-locked"; }
            finally { rw.writeLock().unlock(); }
        }
        return "not acquired";
    }

    @PostMapping("/semaphore/{name}/acquire")
    public String semaphoreAcquire(@PathVariable String name, @RequestParam(defaultValue = "1") int permits) throws InterruptedException {
        RSemaphore sem = redisson.getSemaphore(name);
        sem.trySetPermits(3);                                      // init permits once
        if (sem.tryAcquire(permits, 5, TimeUnit.SECONDS)) {
            try { Thread.sleep(500); return "acquired " + permits; }
            finally { sem.release(permits); }
        }
        return "not acquired";
    }

    @PostMapping("/countdown/{name}/init")
    public String countdownInit(@PathVariable String name, @RequestParam int count) {
        RCountDownLatch latch = redisson.getCountDownLatch(name);
        latch.trySetCount(count);
        return "initialized to " + count;
    }

    @PostMapping("/countdown/{name}/countdown")
    public String countdown(@PathVariable String name) {
        redisson.getCountDownLatch(name).countDown();
        return "counted down";
    }

    @GetMapping("/countdown/{name}/await")
    public String countdownAwait(@PathVariable String name) throws InterruptedException {
        boolean ok = redisson.getCountDownLatch(name).await(10, TimeUnit.SECONDS);
        return ok ? "all counted down" : "timeout";
    }

    @PostMapping("/ratelimiter/{name}")
    public boolean rateLimit(@PathVariable String name) {
        org.redisson.api.RRateLimiter rl = redisson.getRateLimiter(name);
        // 5 permits per 1 second
        rl.trySetRate(org.redisson.api.RateType.OVERALL,
                5, 1, org.redisson.api.RateIntervalUnit.SECONDS);
        return rl.tryAcquire(1);                                    // true if allowed
    }
}
