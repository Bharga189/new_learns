package com.example.redislock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A distributed lock with priority + FIFO ordering of waiters.
 * <p>Waiters are kept in a Redis sorted set scored by
 * {@code priorityTier * 10^12 + arrivalSequence}. High priority uses tier 0,
 * normal uses tier 1, so every high-priority waiter is served before any
 * normal one, while FIFO is preserved inside each tier.
 * <p>When a high-priority request arrives and the lock is held by a normal-priority
 * holder, the high-priority request can preempt and acquire the lock immediately.
 */
public class RedisPriorityLock {

    private static final String TRY_ACQUIRE_SCRIPT =
            "local lockKey = KEYS[1]\n" +
            "local queueKey = KEYS[2]\n" +
            "local token = ARGV[1]\n" +
            "local ttl = tonumber(ARGV[2])\n" +
            "local highPriority = tonumber(ARGV[3])\n" +
            "local owner = redis.call('hget', lockKey, 'owner')\n" +
            "local ownerTier = tonumber(redis.call('hget', lockKey, 'tier'))\n" +
            "if owner == false or owner == nil then\n" +
            "  local head = redis.call('zrange', queueKey, 0, 0)\n" +
            "  if #head == 0 or head[1] == token then\n" +
            "    redis.call('hset', lockKey, 'owner', token, 'count', 1, 'tier', highPriority)\n" +
            "    redis.call('pexpire', lockKey, ttl)\n" +
            "    if #head > 0 then redis.call('zrem', queueKey, token) end\n" +
            "    return 1\n" +
            "  end\n" +
            "  return 0\n" +
            "end\n" +
            "if owner == token then\n" +
            "  redis.call('hincrby', lockKey, 'count', 1)\n" +
            "  redis.call('pexpire', lockKey, ttl)\n" +
            "  return 1\n" +
            "end\n" +
            "-- High priority can preempt normal priority holder\n" +
            "if highPriority == 1 and ownerTier == 0 then\n" +
            "  -- Current holder is normal priority (Java stores tier=0 for normal, tier=1 for high)\n" +
            "  local normalToken = owner\n" +
            "  -- Enqueue the normal holder with priority tier 0 (normal = 0 * 10^12)\n" +
            "  redis.call('zadd', queueKey, 0 * 1000000000000, normalToken)\n" +
            "  -- Set this high-priority token as the new owner with tier 1\n" +
            "  redis.call('hset', lockKey, 'owner', token, 'count', 1, 'tier', 1)\n" +
            "  redis.call('pexpire', lockKey, ttl)\n" +
            "  return 1\n" +
            "end\n" +
            "-- Normal priority request: just wait in queue (will be preempted by high priority)\n" +
            "return 0";

    private static final String ENQUEUE_SCRIPT =
            "local queueKey = KEYS[1]\n" +
            "local seqKey = KEYS[2]\n" +
            "local priority = tonumber(ARGV[1])\n" +
            "local token = ARGV[2]\n" +
            "local seq = redis.call('incr', seqKey)\n" +
            "local score = priority * 1000000000000 + seq\n" +
            "redis.call('zadd', queueKey, score, token)\n" +
            "redis.call('expire', queueKey, 3600)\n" +
            "redis.call('expire', seqKey, 3600)\n" +
            "return seq";

    private static final String RELEASE_SCRIPT =
            "local lockKey = KEYS[1]\n" +
            "local queueKey = KEYS[2]\n" +
            "local token = ARGV[1]\n" +
            "local ttl = tonumber(ARGV[2])\n" +
            "local owner = redis.call('hget', lockKey, 'owner')\n" +
            "if owner ~= token then return -1 end\n" +
            "local c = tonumber(redis.call('hget', lockKey, 'count'))\n" +
            "if c > 1 then\n" +
            "  redis.call('hincrby', lockKey, 'count', -1)\n" +
            "  redis.call('pexpire', lockKey, ttl)\n" +
            "  return 0\n" +
            "end\n" +
            "-- Release: get next waiter from queue, maintaining priority order\n" +
            "local firstWaiter = redis.call('zrange', queueKey, 0, 0)\n" +
            "if #firstWaiter > 0 then\n" +
            "  local nextToken = firstWaiter[1]\n" +
            "  -- Get the score to determine the priority tier\n" +
            "local nextScore = redis.call('zscore', queueKey, nextToken)\n" +
            "  local priorityTier = nextScore ~= false and math.floor(tonumber(nextScore) / 1000000000000) or 1\n" +
            "  -- Remove the waited-for token from the queue\n" +
            "  redis.call('zrem', queueKey, nextToken)\n" +
            "  -- Set the new owner with the appropriate tier\n" +
            "  redis.call('hset', lockKey, 'owner', nextToken, 'count', 1, 'tier', priorityTier)\n" +
            "  redis.call('pexpire', lockKey, ttl)\n" +
            "  return 1\n" +
            "end\n" +
            "-- No more waiters, lock is released\n" +
            "redis.call('del', lockKey)\n" +
            "return 2";

    private final StringRedisTemplate redis;
    private final String lockKey;
    private final String queueKey;
    private final String seqKey;
    private final boolean highPriority;
    private final long waitTime;
    private final long leaseTime;
    private final long retryInterval;

    private final DefaultRedisScript<Long> tryAcquireScript;
    private final DefaultRedisScript<Long> enqueueScript;
    private final DefaultRedisScript<Long> releaseScript;

    private String token;
    private volatile boolean held = false;
    private volatile boolean queued = false;

    public RedisPriorityLock(StringRedisTemplate redis, String lockKey, boolean highPriority,
                             long waitTime, long leaseTime, long retryInterval) {
        this.redis = redis;
        this.lockKey = lockKey;
        this.queueKey = lockKey + ":queue";
        this.seqKey = lockKey + ":seq";
        this.highPriority = highPriority;
        this.waitTime = waitTime;
        this.leaseTime = leaseTime;
        this.retryInterval = retryInterval;

        this.tryAcquireScript = new DefaultRedisScript<>(TRY_ACQUIRE_SCRIPT, Long.class);
        this.enqueueScript = new DefaultRedisScript<>(ENQUEUE_SCRIPT, Long.class);
        this.releaseScript = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
    }

    public boolean tryLock() {
        this.token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitTime;
        while (System.currentTimeMillis() < deadline) {
            Long res = redis.execute(tryAcquireScript, List.of(lockKey, queueKey), token, String.valueOf(leaseTime), String.valueOf(highPriority ? 1 : 0));
            System.out.println("[LOCK] " + lockKey + " token=" + token + " high=" + highPriority + " tryAcquire=" + res + " queue=" + redis.opsForZSet().range(queueKey, 0, -1));
            if (res != null && res == 1L) {
                held = true;
                return true;
            }
            if (!queued) {
                int tier = highPriority ? 0 : 1;
                System.out.println("[LOCK] enqueue " + lockKey + " token=" + token + " tier=" + tier);
                redis.execute(enqueueScript, List.of(queueKey, seqKey), String.valueOf(tier), token);
                queued = true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        // timed out: remove from queue
        if (queued) {
            redis.opsForZSet().remove(queueKey, token);
        }
        return false;
    }

    public void unlock() {
        if (!held || token == null) {
            return;
        }
        try {
            redis.execute(releaseScript, List.of(lockKey, queueKey), token, String.valueOf(leaseTime));
        } finally {
            held = false;
            token = null;
        }
    }

    public boolean isHeld() {
        return held;
    }
}