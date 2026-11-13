package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis STRINGS
 * ------------
 * A Redis String is a binary-safe sequence of bytes. It is the simplest and most
 * common data type and is used for caching values, counters, session tokens, etc.
 *
 * Underlying commands demonstrated: SET, GET, MSET, MGET, INCR, INCRBY, DECR,
 * APPEND, STRLEN, SETEX, SETNX, GETSET, GETDEL.
 */
@RestController
@RequestMapping("/api/redis/strings")
public class RedisStringController {

    private final RedisTemplate<String, Object> template;
    private final ValueOperations<String, Object> ops;

    public RedisStringController(RedisTemplate<String, Object> template) {
        this.template = template;
        this.ops = template.opsForValue();
    }

    @PostMapping("/{key}")
    public String set(@PathVariable String key, @RequestBody Object value,
                      @RequestParam(required = false) Long ttlSeconds) {
        if (ttlSeconds != null) {
            ops.set(key, value, ttlSeconds, TimeUnit.SECONDS);   // SETEX
        } else {
            ops.set(key, value);                                 // SET
        }
        return "OK";
    }

    @GetMapping("/{key}")
    public Object get(@PathVariable String key) {
        return ops.get(key);                                     // GET
    }

    /** SETNX: set only if the key does not already exist (atomic lock primitive). */
    @PostMapping("/{key}/setnx")
    public Boolean setNx(@PathVariable String key, @RequestBody Object value) {
        return ops.setIfAbsent(key, value);                      // SETNX
    }

    /** Atomic counter increment. */
    @PostMapping("/{key}/incr")
    public Long incr(@PathVariable String key, @RequestParam(defaultValue = "1") long by) {
        if (by == 1) return ops.increment(key);                 // INCR
        return ops.increment(key, by);                           // INCRBY
    }

    @PostMapping("/{key}/decr")
    public Long decr(@PathVariable String key, @RequestParam(defaultValue = "1") long by) {
        return ops.increment(key, -by);                          // DECR / DECRBY
    }

    @PostMapping("/mset")
    public String mset(@RequestBody Map<String, Object> values) {
        ops.multiSet(values);                                    // MSET
        return "OK";
    }

    @GetMapping("/mget")
    public Object mget(@RequestParam String... keys) {
        return ops.multiGet(java.util.List.of(keys));             // MGET
    }

    @PostMapping("/{key}/append")
    public Integer append(@PathVariable String key, @RequestParam String suffix) {
        return ops.append(key, suffix);                          // APPEND
    }

    @GetMapping("/{key}/strlen")
    public Long strlen(@PathVariable String key) {
        return ops.size(key);                                    // STRLEN
    }

    @DeleteMapping("/{key}")
    public Boolean delete(@PathVariable String key) {
        return template.delete(key);                                    // DEL
    }
}
