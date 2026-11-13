package com.example.redisdemo.controller;

import com.example.redisdemo.config.RedisPubSubConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Redis PUB/SUB
 * -------------
 * Publish/subscribe messaging. Publishers send messages to a channel; all active
 * subscribers receive them. No persistence, no replay. Use Streams for durable
 * messaging.
 */
@RestController
@RequestMapping("/api/redis/pubsub")
public class RedisPubSubController {

    private final RedisTemplate<String, Object> template;

    public RedisPubSubController(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    @PostMapping("/publish")
    public Long publish(@RequestParam String message) {
        return template.convertAndSend(RedisPubSubConfig.CHANNEL, message); // PUBLISH
    }
}
