package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Redis SORTED SETS (ZSETs)
 * -------------------------
 * Like Sets but each member has a floating-point score used to keep members
 * sorted. Great for leaderboards, priority queues, time-series by timestamp.
 * Commands: ZADD, ZREM, ZSCORE, ZRANK, ZREVRANK, ZRANGE, ZREVRANGE,
 * ZRANGEBYSCORE, ZCARD, ZCOUNT, ZINCRBY, ZPOPMIN, ZPOPMAX.
 */
@RestController
@RequestMapping("/api/redis/zsets")
public class RedisZSetController {

    private final ZSetOperations<String, Object> ops;

    public RedisZSetController(RedisTemplate<String, Object> template) {
        this.ops = template.opsForZSet();
    }

    @PostMapping("/{key}")
    public Boolean zadd(@PathVariable String key, @RequestParam double score,
                        @RequestBody Object value) {
        return ops.add(key, value, score);                           // ZADD
    }

    @DeleteMapping("/{key}")
    public Long zrem(@PathVariable String key, @RequestBody Object... values) {
        return ops.remove(key, values);                              // ZREM
    }

    @GetMapping("/{key}/score/{value}")
    public Double zscore(@PathVariable String key, @PathVariable Object value) {
        return ops.score(key, value);                                // ZSCORE
    }

    @GetMapping("/{key}/rank/{value}")
    public Long zrank(@PathVariable String key, @PathVariable Object value) {
        return ops.rank(key, value);                                 // ZRANK (ascending)
    }

    @GetMapping("/{key}")
    public Set<ZSetOperations.TypedTuple<Object>> zrange(
            @PathVariable String key,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "-1") long end) {
        return ops.rangeWithScores(key, start, end);                 // ZRANGE WITHSCORES
    }

    @GetMapping("/{key}/byscore")
    public Set<Object> zrangeByScore(@PathVariable String key,
                                     @RequestParam double min, @RequestParam double max) {
        return ops.rangeByScore(key, min, max);                     // ZRANGEBYSCORE
    }

    @GetMapping("/{key}/size")
    public Long zcard(@PathVariable String key) {
        return ops.size(key);                                       // ZCARD
    }

    @GetMapping("/{key}/count")
    public Long zcount(@PathVariable String key, @RequestParam double min, @RequestParam double max) {
        return ops.count(key, min, max);                             // ZCOUNT
    }

    @PostMapping("/{key}/incr/{value}")
    public Double zincrby(@PathVariable String key, @PathVariable Object value,
                          @RequestParam double by) {
        return ops.incrementScore(key, value, by);                   // ZINCRBY
    }

    @GetMapping("/{key}/popmin")
    public ZSetOperations.TypedTuple<Object> zpopmin(@PathVariable String key) {
        return ops.popMin(key);                                     // ZPOPMIN
    }

    @GetMapping("/{key}/popmax")
    public ZSetOperations.TypedTuple<Object> zpopmax(@PathVariable String key) {
        return ops.popMax(key);                                     // ZPOPMAX
    }
}
