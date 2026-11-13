package com.example.redisdemo.service;

import com.example.redisdemo.model.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstrates Spring's Cache abstraction (@Cacheable/@CachePut/@CacheEvict)
 * backed by RedisCacheManager.
 */
@Service
public class UserService {

    private final Map<Long, User> db = new ConcurrentHashMap<>();

    public UserService() {
        db.put(1L, new User(1L, "Alice", "alice@example.com", Instant.now().toString()));
        db.put(2L, new User(2L, "Bob", "bob@example.com", Instant.now().toString()));
    }

    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        // Simulate slow DB lookup; result is cached in Redis.
        simulateSlow();
        return db.get(id);
    }

    @CachePut(value = "users", key = "#user.id")
    public User update(User user) {
        db.put(user.getId(), user);
        return user;                                                   // refreshes cache
    }

    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        db.remove(id);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void clearCache() {
    }

    private void simulateSlow() {
        try { Thread.sleep(200); } catch (InterruptedException ignored) { }
    }
}
