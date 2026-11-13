package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis TRANSACTIONS
 * ------------------
 * MULTI ... EXEC groups commands so they run atomically (no other client's
 * commands interleave). WATCH adds optimistic locking (CAS). Spring Data Redis
 * exposes this via SessionCallback / @Transactional.
 */
@RestController
@RequestMapping("/api/redis/tx")
public class RedisTransactionController {

    private final RedisTemplate<String, Object> template;

    public RedisTransactionController(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    @PostMapping("/transfer")
    public List<Object> transfer(@RequestParam String from, @RequestParam String to,
                                @RequestParam long amount) {
        // Atomic transfer of "amount" from one counter to another.
        return template.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(org.springframework.data.redis.core.RedisOperations operations) {
                operations.multi();                                    // MULTI
                operations.opsForValue().increment(from, -amount);
                operations.opsForValue().increment(to, amount);
                return operations.exec();                              // EXEC
            }
        });
    }

    /** WATCH-based optimistic lock (CAS). */
    @PostMapping("/cas")
    public String cas(@RequestParam String key, @RequestParam long expected, @RequestParam long newValue) {
        return template.execute(new SessionCallback<>() {
            @Override
            public String execute(org.springframework.data.redis.core.RedisOperations operations) {
                operations.watch(key);                                  // WATCH
                Object cur = operations.opsForValue().get(key);
                long current = cur == null ? 0L : Long.parseLong(cur.toString());
                if (current != expected) {
                    operations.unwatch();
                    return "CONFLICT: current=" + current;
                }
                operations.multi();
                operations.opsForValue().set(key, newValue);
                List<Object> res = operations.exec();                   // EXEC (null if watched key changed)
                return res == null ? "ABORTED" : "OK";
            }
        });
    }
}
