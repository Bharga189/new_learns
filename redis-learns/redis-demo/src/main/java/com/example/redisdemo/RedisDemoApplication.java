package com.example.redisdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Redis + Redisson feature demo.
 *
 * <p>Run with: {@code mvn spring-boot:run} after starting Redis via Podman (see README.md).
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class RedisDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisDemoApplication.class, args);
    }
}
