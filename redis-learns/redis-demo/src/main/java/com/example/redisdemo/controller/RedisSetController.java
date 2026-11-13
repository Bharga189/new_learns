package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Redis SETS
 * ----------
 * An unordered collection of unique strings. Great for tags, unique visitors,
 * friend lists, etc. Commands: SADD, SREM, SMEMBERS, SISMEMBER, SCARD,
 * SPOP, SRANDMEMBER, SUNION, SINTER, SDIFF, SMOVE.
 */
@RestController
@RequestMapping("/api/redis/sets")
public class RedisSetController {

    private final SetOperations<String, Object> ops;

    public RedisSetController(RedisTemplate<String, Object> template) {
        this.ops = template.opsForSet();
    }

    @PostMapping("/{key}")
    public Long sadd(@PathVariable String key, @RequestBody Object... values) {
        return ops.add(key, values);                                 // SADD
    }

    @DeleteMapping("/{key}")
    public Long srem(@PathVariable String key, @RequestBody Object... values) {
        return ops.remove(key, values);                              // SREM
    }

    @GetMapping("/{key}")
    public Set<Object> smembers(@PathVariable String key) {
        return ops.members(key);                                    // SMEMBERS
    }

    @GetMapping("/{key}/contains/{value}")
    public Boolean sismember(@PathVariable String key, @PathVariable Object value) {
        return ops.isMember(key, value);                            // SISMEMBER
    }

    @GetMapping("/{key}/size")
    public Long scard(@PathVariable String key) {
        return ops.size(key);                                       // SCARD
    }

    @GetMapping("/{key}/pop")
    public Object spop(@PathVariable String key) {
        return ops.pop(key);                                        // SPOP
    }

    @GetMapping("/{key}/random")
    public Object srandmember(@PathVariable String key) {
        return ops.randomMember(key);                               // SRANDMEMBER
    }

    @GetMapping("/union")
    public Set<Object> sunion(@RequestParam String key1, @RequestParam String key2) {
        return ops.union(key1, key2);                               // SUNION
    }

    @GetMapping("/inter")
    public Set<Object> sinter(@RequestParam String key1, @RequestParam String key2) {
        return ops.intersect(key1, key2);                           // SINTER
    }

    @GetMapping("/diff")
    public Set<Object> sdiff(@RequestParam String key1, @RequestParam String key2) {
        return ops.difference(key1, key2);                          // SDIFF
    }
}
