package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

/**
 * Redis BITFIELDS / BITMAPS and HYPERLOGLOG
 * ----------------------------------------
 * Bitmaps are strings treated as arrays of bits (SETBIT/GETBIT/BITCOUNT).
 * Useful for daily active users, feature flags, etc.
 * HyperLogLog estimates cardinality of a set with tiny memory (PFADD/PFCOUNT).
 */
@RestController
@RequestMapping("/api/redis/bits")
public class RedisBitController {

    private final ValueOperations<String, Object> ops;
    private final RedisTemplate<String, Object> template;

    public RedisBitController(RedisTemplate<String, Object> template) {
        this.template = template;
        this.ops = template.opsForValue();
    }

    @PostMapping("/{key}/bit/{offset}")
    public Boolean setbit(@PathVariable String key, @PathVariable long offset, @RequestParam boolean value) {
        return template.opsForValue().setBit(key, offset, value);    // SETBIT
    }

    @GetMapping("/{key}/bit/{offset}")
    public Boolean getbit(@PathVariable String key, @PathVariable long offset) {
        return template.opsForValue().getBit(key, offset);           // GETBIT
    }

    @GetMapping("/{key}/bitcount")
    public Long bitcount(@PathVariable String key) {
        return template.execute((org.springframework.data.redis.core.RedisCallback<Long>)
                c -> c.stringCommands().bitCount(key.getBytes()));   // BITCOUNT
    }

    // HyperLogLog
    @PostMapping("/hll/{key}")
    public Long pfadd(@PathVariable String key, @RequestBody Object... values) {
        return template.opsForHyperLogLog().add(key, values);        // PFADD
    }

    @GetMapping("/hll/{key}/count")
    public Long pfcount(@PathVariable String key) {
        return template.opsForHyperLogLog().size(key);               // PFCOUNT
    }
}
