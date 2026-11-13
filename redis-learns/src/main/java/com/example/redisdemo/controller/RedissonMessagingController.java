package com.example.redisdemo.controller;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

/**
 * Redisson MESSAGING (RTopic)
 * ---------------------------
 * RTopic is Redisson's pub/sub abstraction with POJO message support and
 * (optionally) reliable delivery via RReliableTopic. Messages are serialized
 * with the configured codec.
 */
@RestController
@RequestMapping("/api/redisson/topic")
public class RedissonMessagingController {

    private final RedissonClient redisson;

    public RedissonMessagingController(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @PostMapping("/publish")
    public long publish(@RequestParam String channel, @RequestParam String message) {
        RTopic topic = redisson.getTopic(channel);
        return topic.publish(message);                              // PUBLISH
    }

    /** Subscribe (logs incoming messages). Call once to register a listener. */
    @PostMapping("/subscribe/{channel}")
    public String subscribe(@PathVariable String channel) {
        RTopic topic = redisson.getTopic(channel);
        topic.addListener(String.class, (ch, msg) ->
                System.out.println("[RTOPIC] channel=" + ch + " msg=" + msg));
        return "subscribed to " + channel;
    }
}
