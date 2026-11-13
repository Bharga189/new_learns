package com.example.redisdemo.controller;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Redis STREAMS
 * -------------
 * An append-only log with consumer groups, similar to Kafka topics but built into
 * Redis. Great for event sourcing, message queues, activity feeds.
 * Commands: XADD, XREAD, XREADGROUP, XGROUP CREATE, XACK, XLEN, XRANGE, XDEL.
 */
@RestController
@RequestMapping("/api/redis/streams")
public class RedisStreamController {

    private final RedisTemplate<String, Object> template;

    public RedisStreamController(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    @PostMapping("/{stream}")
    public String xadd(@PathVariable String stream, @RequestBody Map<String, Object> fields) {
        RecordId id = template.opsForStream().add(stream, fields);   // XADD
        return id.getValue();
    }

    @GetMapping("/{stream}")
    public List<MapRecord<String, Object, Object>> xrange(@PathVariable String stream,
                                                          @RequestParam(defaultValue = "-") String start,
                                                          @RequestParam(defaultValue = "+") String end) {
        return template.opsForStream().range(stream, Range.unbounded()); // XRANGE
    }

    @GetMapping("/{stream}/len")
    public Long xlen(@PathVariable String stream) {
        return template.opsForStream().size(stream);                 // XLEN
    }

    @PostMapping("/{stream}/group")
    public String createGroup(@PathVariable String stream, @RequestParam String group) {
        try {
            template.opsForStream().createGroup(stream, group);      // XGROUP CREATE
            return "OK";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @GetMapping("/{stream}/readgroup")
    public List<MapRecord<String, Object, Object>> readGroup(@PathVariable String stream,
                                                            @RequestParam String group,
                                                            @RequestParam String consumer) {
        return template.opsForStream().read(Consumer.from(group, consumer),
                StreamReadOptions.empty().count(10).block(java.time.Duration.ofSeconds(1)),
                StreamOffset.create(stream, ReadOffset.lastConsumed())); // XREADGROUP
    }

    @PostMapping("/{stream}/ack")
    public Long xack(@PathVariable String stream, @RequestParam String group,
                     @RequestParam String... ids) {
        return template.opsForStream().acknowledge(stream, group, ids); // XACK
    }
}
