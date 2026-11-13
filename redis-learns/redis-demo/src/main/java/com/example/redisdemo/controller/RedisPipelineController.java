package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis PIPELINING
 * ----------------
 * Sends many commands in one round-trip without waiting for each reply, then
 * reads all replies. Dramatically reduces latency for bulk operations.
 * Not atomic (unlike MULTI/EXEC) but very fast.
 */
@RestController
@RequestMapping("/api/redis/pipeline")
public class RedisPipelineController {

    private final RedisTemplate<String, Object> template;

    public RedisPipelineController(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    @PostMapping("/bulk-set")
    public String bulkSet(@RequestParam int count) {
        List<Object> results = template.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (int i = 0; i < count; i++) {
                connection.stringCommands().set(("pipe:key:" + i).getBytes(),
                        ("value-" + i).getBytes());
            }
            return null;
        });
        return "pipelined " + results.size() + " commands";
    }
}
