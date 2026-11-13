package com.example.redisdemo.controller;

import org.redisson.api.*;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Redisson ADVANCED FEATURES
 * --------------------------
 * Atomic counters, bitsets, HyperLogLog, geo, streams, bloom filters,
 * executor services and scheduled tasks — all distributed.
 */
@RestController
@RequestMapping("/api/redisson/advanced")
public class RedissonAdvancedController {

    private final RedissonClient redisson;

    public RedissonAdvancedController(RedissonClient redisson) {
        this.redisson = redisson;
    }

    // ---------- RAtomicLong ----------
    @PostMapping("/atomiclong/{name}/set")
    public void atomicSet(@PathVariable String name, @RequestParam long v) {
        redisson.getAtomicLong(name).set(v);
    }
    @PostMapping("/atomiclong/{name}/incr")
    public long atomicIncr(@PathVariable String name) {
        return redisson.getAtomicLong(name).incrementAndGet();
    }
    @GetMapping("/atomiclong/{name}")
    public long atomicGet(@PathVariable String name) {
        return redisson.getAtomicLong(name).get();
    }

    // ---------- RAtomicDouble ----------
    @PostMapping("/atomicdouble/{name}/add")
    public double atomicDoubleAdd(@PathVariable String name, @RequestParam double v) {
        RAtomicDouble d = redisson.getAtomicDouble(name);
        d.addAndGet(v);
        return d.get();
    }

    // ---------- RBitSet ----------
    @PostMapping("/bitset/{name}/set/{i}")
    public void bitsetSet(@PathVariable String name, @PathVariable long i) {
        redisson.getBitSet(name).set(i);
    }
    @GetMapping("/bitset/{name}/get/{i}")
    public boolean bitsetGet(@PathVariable String name, @PathVariable long i) {
        return redisson.getBitSet(name).get(i);
    }
    @GetMapping("/bitset/{name}/count")
    public long bitsetCount(@PathVariable String name) {
        return redisson.getBitSet(name).cardinality();
    }

    // ---------- RHyperLogLog ----------
    @PostMapping("/hll/{name}")
    public boolean hllAdd(@PathVariable String name, @RequestBody Object... values) {
        return redisson.getHyperLogLog(name).add(values);
    }
    @GetMapping("/hll/{name}/count")
    public long hllCount(@PathVariable String name) {
        return redisson.getHyperLogLog(name).count();
    }

    // ---------- RGeo ----------
    @PostMapping("/geo/{name}")
    public long geoAdd(@PathVariable String name, @RequestParam double lon, @RequestParam double lat,
                       @RequestBody Object member) {
        return redisson.getGeo(name).add(lon, lat, member);
    }
    @GetMapping("/geo/{name}/radius")
    public Object geoRadius(@PathVariable String name, @RequestParam double lon,
                            @RequestParam double lat, @RequestParam double km) {
        return redisson.getGeo(name).radius(lon, lat, km, GeoUnit.KILOMETERS);
    }

    // ---------- RStream ----------
    @PostMapping("/stream/{name}")
    public String streamAdd(@PathVariable String name, @RequestBody Map<String, Object> fields) {
        org.redisson.api.RStream<String, Object> stream =
                redisson.getStream(name, new org.redisson.client.codec.StringCodec());
        StreamMessageId id = stream.add(StreamAddArgs.entries(fields));
        return id.toString();
    }
    @GetMapping("/stream/{name}")
    public Object streamRange(@PathVariable String name) {
        return redisson.getStream(name, new org.redisson.client.codec.StringCodec())
                .range(StreamMessageId.MIN, StreamMessageId.MAX);
    }

    // ---------- RBloomFilter ----------
    @PostMapping("/bloom/{name}/init")
    public boolean bloomInit(@PathVariable String name, @RequestParam long capacity, @RequestParam double errorRate) {
        RBloomFilter<Object> bf = redisson.getBloomFilter(name);
        return bf.tryInit(capacity, errorRate);
    }
    @PostMapping("/bloom/{name}")
    public void bloomAdd(@PathVariable String name, @RequestBody Object value) {
        redisson.getBloomFilter(name).add(value);
    }
    @GetMapping("/bloom/{name}/contains/{value}")
    public boolean bloomContains(@PathVariable String name, @PathVariable Object value) {
        return redisson.getBloomFilter(name).contains(value);
    }

    // ---------- RExecutorService ----------
    @PostMapping("/executor/{name}/submit")
    public String submitTask(@PathVariable String name) {
        RExecutorService exec = redisson.getExecutorService(name);
        // Register a simple runnable task class name; here we use a lambda via RRunnable not serializable across nodes,
        // so instead demonstrate scheduling a local task handle.
        java.util.concurrent.Future<?> f = exec.submit(() -> {
            System.out.println("executed on worker");
        });
        return "submitted: " + f;
    }

    // ---------- RScheduledExecutorService ----------
    @PostMapping("/scheduled/{name}")
    public String schedule(@PathVariable String name) {
        RScheduledExecutorService sexec = redisson.getExecutorService(name);
        sexec.schedule(() -> System.out.println("scheduled task ran"), 2, java.util.concurrent.TimeUnit.SECONDS);
        return "scheduled";
    }
}
