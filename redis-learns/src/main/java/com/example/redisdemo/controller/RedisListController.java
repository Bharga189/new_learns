package com.example.redisdemo.controller;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis LISTS
 * -----------
 * A List is an ordered collection of strings (linked list). Supports push/pop
 * from both ends, making it suitable for queues, stacks and activity feeds.
 * Commands: LPUSH, RPUSH, LPOP, RPOP, LRANGE, LLEN, LINDEX, LSET, LINSERT,
 * LREM, LTRIM, RPOPLPUSH / LMOVE.
 */
@RestController
@RequestMapping("/api/redis/lists")
public class RedisListController {

    private final ListOperations<String, Object> ops;

    public RedisListController(RedisTemplate<String, Object> template) {
        this.ops = template.opsForList();
    }

    @PostMapping("/{key}/left")
    public Long lpush(@PathVariable String key, @RequestBody Object value) {
        return ops.leftPush(key, value);                             // LPUSH
    }

    @PostMapping("/{key}/right")
    public Long rpush(@PathVariable String key, @RequestBody Object value) {
        return ops.rightPush(key, value);                            // RPUSH
    }

    @GetMapping("/{key}/left")
    public Object lpop(@PathVariable String key) {
        return ops.leftPop(key);                                    // LPOP
    }

    @GetMapping("/{key}/right")
    public Object rpop(@PathVariable String key) {
        return ops.rightPop(key);                                   // RPOP
    }

    @GetMapping("/{key}")
    public List<Object> range(@PathVariable String key,
                              @RequestParam(defaultValue = "0") long start,
                              @RequestParam(defaultValue = "-1") long end) {
        return ops.range(key, start, end);                          // LRANGE
    }

    @GetMapping("/{key}/size")
    public Long llen(@PathVariable String key) {
        return ops.size(key);                                       // LLEN
    }

    @GetMapping("/{key}/index/{i}")
    public Object lindex(@PathVariable String key, @PathVariable long i) {
        return ops.index(key, i);                                   // LINDEX
    }

    @PutMapping("/{key}/index/{i}")
    public void lset(@PathVariable String key, @PathVariable long i, @RequestBody Object value) {
        ops.set(key, i, value);                                      // LSET
    }

    @DeleteMapping("/{key}/remove")
    public Long lrem(@PathVariable String key, @RequestParam Object value,
                     @RequestParam(defaultValue = "0") long count) {
        return ops.remove(key, count, value);                        // LREM
    }

    @PostMapping("/{key}/trim")
    public void ltrim(@PathVariable String key, @RequestParam long start, @RequestParam long end) {
        ops.trim(key, start, end);                                  // LTRIM
    }
}
