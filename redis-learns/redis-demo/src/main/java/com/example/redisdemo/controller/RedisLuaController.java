package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

/**
 * Redis LUA SCRIPTING (EVAL)
 * --------------------------
 * Run a Lua script atomically on the server. Useful for complex conditional
 * updates that would otherwise need multiple round-trips and races.
 */
@RestController
@RequestMapping("/api/redis/lua")
public class RedisLuaController {

    private final RedisTemplate<String, Object> template;

    public RedisLuaController(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    /** Atomic "decrement if enough stock" script returning new stock or -1. */
    @PostMapping("/decrement-if-enough")
    public Object decrement(@RequestParam String key, @RequestParam long amount) {
        String script = """
                local current = redis.call('GET', KEYS[1])
                if current == false then current = 0 else current = tonumber(current) end
                if current >= tonumber(ARGV[1]) then
                    redis.call('DECRBY', KEYS[1], ARGV[1])
                    return current - tonumber(ARGV[1])
                else
                    return -1
                end
                """;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        return template.execute(redisScript, java.util.List.of(key), amount);
    }

    /** Rate limiter-ish: INCR with TTL using a Lua script. */
    @PostMapping("/incr-with-ttl")
    public Object incrWithTtl(@RequestParam String key, @RequestParam long ttlSeconds) {
        String script = """
                local c = redis.call('INCR', KEYS[1])
                if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
                return c
                """;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        return template.execute(redisScript, java.util.List.of(key), ttlSeconds);
    }
}
