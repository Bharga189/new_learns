package com.example.redisdemo.controller;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Redis HASHES
 * ------------
 * A Hash is a collection of field-value pairs (like a Java Map / JSON object).
 * Ideal for representing objects (e.g. a User with name, email, age) without
 * serializing the whole object. Commands: HSET, HGET, HMSET, HMGET, HGETALL,
 * HDEL, HEXISTS, HINCRBY, HKEYS, HVALS, HLEN, HSETNX.
 */
@RestController
@RequestMapping("/api/redis/hashes")
public class RedisHashController {

    private final HashOperations<String, Object, Object> ops;

    public RedisHashController(RedisTemplate<String, Object> template) {
        this.ops = template.opsForHash();
    }

    @PostMapping("/{key}")
    public Long hset(@PathVariable String key, @RequestBody Map<String, Object> fields) {
        ops.putAll(key, fields);                                    // HMSET / HSET multiple
        return (long) fields.size();
    }

    @PostMapping("/{key}/{field}")
    public void hsetField(@PathVariable String key, @PathVariable String field,
                          @RequestBody Object value) {
        ops.put(key, field, value);                                 // HSET
    }

    @GetMapping("/{key}/{field}")
    public Object hget(@PathVariable String key, @PathVariable String field) {
        return ops.get(key, field);                                 // HGET
    }

    @GetMapping("/{key}")
    public Map<Object, Object> hgetAll(@PathVariable String key) {
        return ops.entries(key);                                    // HGETALL
    }

    @GetMapping("/{key}/fields")
    public Object hkeys(@PathVariable String key) {
        return ops.keys(key);                                       // HKEYS
    }

    @GetMapping("/{key}/values")
    public Object hvals(@PathVariable String key) {
        return ops.values(key);                                     // HVALS
    }

    @GetMapping("/{key}/size")
    public Long hlen(@PathVariable String key) {
        return ops.size(key);                                       // HLEN
    }

    @GetMapping("/{key}/{field}/exists")
    public Boolean hexists(@PathVariable String key, @PathVariable String field) {
        return ops.hasKey(key, field);                              // HEXISTS
    }

    @DeleteMapping("/{key}/{field}")
    public Long hdel(@PathVariable String key, @PathVariable String field) {
        return ops.delete(key, field);                              // HDEL
    }

    @PostMapping("/{key}/{field}/incr")
    public Long hincrBy(@PathVariable String key, @PathVariable String field,
                        @RequestParam(defaultValue = "1") long by) {
        return ops.increment(key, field, by);                        // HINCRBY
    }
}
