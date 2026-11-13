package com.example.redislock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class RedisLockAspect {

    private final RedisLockManager manager;
    private final RedisLockProperties properties;

    @Autowired
    public RedisLockAspect(RedisLockManager manager, RedisLockProperties properties) {
        this.manager = manager;
        this.properties = properties;
    }

    @Around("@annotation(com.example.redislock.RedisLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        RedisLock redisLock = method.getAnnotation(RedisLock.class);
        if (redisLock == null) {
            return pjp.proceed();
        }

        String key = manager.buildKey(redisLock.prefix(), redisLock.keys(), method, pjp.getArgs());
        long waitTime = redisLock.waitTime() > 0 ? redisLock.waitTime() : properties.getWaitTime();
        long leaseTime = redisLock.leaseTime() > 0 ? redisLock.leaseTime() : properties.getLeaseTime();

        RedisPriorityLock lock = manager.createLock(key, redisLock.highPriority(), waitTime, leaseTime);
        if (!lock.tryLock()) {
            throw new IllegalStateException("Could not acquire redis lock: " + key);
        }
        try {
            return pjp.proceed();
        } finally {
            lock.unlock();
        }
    }
}
