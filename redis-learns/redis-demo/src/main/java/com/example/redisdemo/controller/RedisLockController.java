package com.example.redisdemo.controller;

import com.example.redisdemo.service.RedisLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lock")
public class RedisLockController {

    @Autowired
    private RedisLockService lockService;

    @PostMapping("/acquire")
    public String acquireLock(@RequestParam String lockKey,
                              @RequestParam(defaultValue = "false") boolean highPriority) throws Exception {
        String result = lockService.acquireLock(lockKey, highPriority);
        return result;
    }

    @PostMapping("/release")
    public String releaseLock(@RequestParam String lockKey) {
        lockService.releaseLock(lockKey);
        return "Lock released: " + lockKey;
    }

    @GetMapping("/status/{lockKey}")
    public String lockStatus(@PathVariable String lockKey) {
        boolean held = lockService.isLockHeld(lockKey);
        return "Lock " + lockKey + " status: " + (held ? "HELD" : "FREE");
    }
}