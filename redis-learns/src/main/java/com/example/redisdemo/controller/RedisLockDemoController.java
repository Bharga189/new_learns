package com.example.redisdemo.controller;

import com.example.redisdemo.service.LockDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lock")
public class RedisLockDemoController {

    private final LockDemoService service;

    public RedisLockDemoController(LockDemoService service) {
        this.service = service;
    }

    @GetMapping("/process/{id}")
    public String process(@PathVariable String id,
                          @RequestParam(defaultValue = "false") boolean high) throws InterruptedException {
        if (high) {
            return service.processHigh(id);
        }
        return service.process(id);
    }
}
