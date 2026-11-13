package com.example.redislock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative distributed lock backed by Redis with priority-based FIFO ordering.
 *
 * <p>Locks are stored in Redis sorted sets with scoring {@code priorityTier * 10^12 + arrivalSequence}.
 * High priority (tier 1) waiters are served before normal priority (tier 0) waiters,
 * while FIFO order is preserved within each tier.
 *
 * <p>{@code keys} is an array of String expressions (SpEL supported, e.g.
 * {@code "#id"}, {@code "order:{#orderId}"}) that are evaluated against the
 * method arguments and joined with ':' to form the lock key.
 *
 * <p>{@code prefix} overrides the default key prefix from configuration.
 *
 * <p>{@code highPriority()} - When true, the request jumps ahead of pending
 * normal-priority requests. High-priority requests preempt normal-priority
 * lock holders and are FIFO-ordered among themselves.
 *
 * <p>Example usage:
 * {@code @RedisLock(keys = {"#id"}, highPriority = true)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    /** Array of key expressions. */
    String[] keys() default {};

    /** Optional per-annotation key prefix override. */
    String prefix() default "";

    /** When true the request jumps ahead of pending normal-priority requests. */
    boolean highPriority() default false;

    /** Override wait time (ms). -1 means use configured default. */
    long waitTime() default -1L;

    /** Override lease time (ms). -1 means use configured default. */
    long leaseTime() default -1L;
}
