package com.example.redisdemo.controller;

import com.example.redisdemo.model.User;
import com.example.redisdemo.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * Spring CACHE ABSTRACTION on top of Redis.
 * First call hits the (simulated slow) DB; subsequent calls return from Redis.
 */
@RestController
@RequestMapping("/api/redis/cache")
public class RedisCacheController {

    private final UserService userService;

    public RedisCacheController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}")
    public User get(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/users")
    public User put(@RequestBody User user) {
        return userService.update(user);
    }

    @DeleteMapping("/users/{id}")
    public String evict(@PathVariable Long id) {
        userService.delete(id);
        return "evicted";
    }

    @DeleteMapping("/users")
    public String clear() {
        userService.clearCache();
        return "cleared";
    }
}
