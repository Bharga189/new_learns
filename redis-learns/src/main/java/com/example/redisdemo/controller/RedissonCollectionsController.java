package com.example.redisdemo.controller;

import org.redisson.api.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Redisson DISTRIBUTED COLLECTIONS
 * --------------------------------
 * Redisson wraps Redis data structures into familiar Java collection interfaces
 * (Map, List, Set, Queue, Deque, SortedSet) that are distributed and thread-safe
 * across JVMs. Unlike Spring Data Redis (raw commands), Redisson objects behave
 * like in-memory Java collections and support TTL, listeners, and advanced
 * features (locks inside collections, etc.).
 */
@RestController
@RequestMapping("/api/redisson/collections")
public class RedissonCollectionsController {

    private final RedissonClient redisson;

    public RedissonCollectionsController(RedissonClient redisson) {
        this.redisson = redisson;
    }

    // ---------- RBucket (atomic object holder) ----------
    @PostMapping("/bucket/{name}")
    public String bucketSet(@PathVariable String name, @RequestBody Object value) {
        RBucket<Object> bucket = redisson.getBucket(name);
        bucket.set(value);                                          // SET
        return "OK";
    }
    @GetMapping("/bucket/{name}")
    public Object bucketGet(@PathVariable String name) {
        return redisson.getBucket(name).get();                      // GET
    }

    // ---------- RMap ----------
    @PostMapping("/map/{name}")
    public Object mapPut(@PathVariable String name, @RequestParam String key, @RequestBody Object value) {
        RMap<String, Object> map = redisson.getMap(name);
        return map.put(key, value);                                 // HSET
    }
    @GetMapping("/map/{name}/{key}")
    public Object mapGet(@PathVariable String name, @PathVariable String key) {
        return redisson.getMap(name).get(key);
    }
    @GetMapping("/map/{name}")
    public Object mapAll(@PathVariable String name) {
        return redisson.getMap(name).readAllMap();                   // HGETALL
    }

    // ---------- RMapCache (entries with per-key TTL) ----------
    @PostMapping("/mapcache/{name}")
    public Object mapCachePut(@PathVariable String name, @RequestParam String key,
                             @RequestBody Object value, @RequestParam long ttlSeconds) {
        RMapCache<String, Object> mc = redisson.getMapCache(name);
        return mc.put(key, value, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS); // HSET + EXPIRE
    }

    // ---------- RList ----------
    @PostMapping("/list/{name}")
    public boolean listAdd(@PathVariable String name, @RequestBody Object value) {
        return redisson.getList(name).add(value);                   // RPUSH
    }
    @GetMapping("/list/{name}")
    public List<Object> listAll(@PathVariable String name) {
        return redisson.getList(name).readAll();                    // LRANGE
    }

    // ---------- RSet ----------
    @PostMapping("/set/{name}")
    public boolean setAdd(@PathVariable String name, @RequestBody Object value) {
        return redisson.getSet(name).add(value);                    // SADD
    }
    @GetMapping("/set/{name}")
    public Set<Object> setAll(@PathVariable String name) {
        return redisson.getSet(name).readAll();                    // SMEMBERS
    }

    // ---------- RSetCache (entries with TTL) ----------
    @PostMapping("/setcache/{name}")
    public boolean setCacheAdd(@PathVariable String name, @RequestBody Object value, @RequestParam long ttlSeconds) {
        return redisson.getSetCache(name).add(value, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    // ---------- RSortedSet (natural ordering) ----------
    @PostMapping("/sortedset/{name}")
    public boolean sortedSetAdd(@PathVariable String name, @RequestBody Comparable value) {
        return redisson.getSortedSet(name).add(value);              // ZADD (score = natural)
    }

    // ---------- RScoredSortedSet (scored) ----------
    @PostMapping("/scoredset/{name}")
    public boolean scoredSetAdd(@PathVariable String name, @RequestParam double score, @RequestBody Object value) {
        return redisson.getScoredSortedSet(name).add(score, value); // ZADD
    }
    @GetMapping("/scoredset/{name}")
    public Collection<Object> scoredSetRange(@PathVariable String name,
                                            @RequestParam(defaultValue = "0") int start,
                                            @RequestParam(defaultValue = "-1") int end) {
        return redisson.getScoredSortedSet(name).valueRange(start, end); // ZRANGE
    }

    // ---------- RQueue ----------
    @PostMapping("/queue/{name}")
    public boolean queueOffer(@PathVariable String name, @RequestBody Object value) {
        return redisson.getQueue(name).offer(value);                // RPUSH
    }
    @GetMapping("/queue/{name}")
    public Object queuePoll(@PathVariable String name) {
        return redisson.getQueue(name).poll();                      // LPOP
    }

    // ---------- RDeque ----------
    @PostMapping("/deque/{name}/left")
    public void dequePushLeft(@PathVariable String name, @RequestBody Object value) {
        redisson.getDeque(name).addFirst(value);                    // LPUSH
    }
    @PostMapping("/deque/{name}/right")
    public void dequePushRight(@PathVariable String name, @RequestBody Object value) {
        redisson.getDeque(name).addLast(value);                     // RPUSH
    }

    // ---------- RBlockingQueue ----------
    @GetMapping("/blockingqueue/{name}")
    public Object blockingPoll(@PathVariable String name, @RequestParam(defaultValue = "5") long seconds) throws InterruptedException {
        return redisson.getBlockingQueue(name).poll(seconds, java.util.concurrent.TimeUnit.SECONDS); // BLPOP
    }

    // ---------- RMultiMap ----------
    @PostMapping("/multimap/{name}")
    public boolean multimapPut(@PathVariable String name, @RequestParam String key, @RequestBody Object value) {
        return redisson.getListMultimap(name).put(key, value);          // list-valued map
    }
    @GetMapping("/multimap/{name}/{key}")
    public Collection<Object> multimapGet(@PathVariable String name, @PathVariable String key) {
        return redisson.getListMultimap(name).get(key);
    }
}
