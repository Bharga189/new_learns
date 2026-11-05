package com.example.java21.virtualthreads;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 21: Virtual Threads (JEP 444)
 *
 * <p>Java 20: virtual threads were available only as a preview feature
 * (requiring {@code --enable-preview}). Production code typically used platform
 * threads directly or thread pools sized carefully for blocking I/O, because
 * platform threads are relatively expensive OS-backed resources.
 *
 * <p>Java 21: virtual threads are final. A virtual thread is a lightweight
 * thread managed by the JDK; many virtual threads can run on a smaller number
 * of carrier (platform) threads. They are well suited to high-concurrency,
 * blocking-I/O style tasks while keeping the familiar thread-per-request
 * programming model.
 *
 * <p>This example runs small, fast tasks and waits for completion so it stays a
 * deterministic smoke test (no sleeps, no network).
 */
public class VirtualThreadsExample {

    public static void main(String[] args) throws Exception {
        // Java 20 style: platform threads (OS-backed), usually pooled for scale.
        int tasks = 20;
        CountDownLatch platformLatch = new CountDownLatch(tasks);
        AtomicInteger platformSum = new AtomicInteger();
        for (int i = 0; i < tasks; i++) {
            final int value = i;
            Thread platform = new Thread(() -> {
                platformSum.addAndGet(value);
                platformLatch.countDown();
            });
            platform.start();
        }
        platformLatch.await(5, TimeUnit.SECONDS);
        System.out.println("Java 20 style platform threads sum: " + platformSum.get());

        // Java 21: virtual threads are lightweight and cheap to create.
        AtomicInteger virtualSum = new AtomicInteger();
        Thread virtual = Thread.ofVirtual().name("demo-virtual").start(() -> virtualSum.addAndGet(42));
        virtual.join(Duration.ofSeconds(5));
        System.out.println("Java 21 single virtual thread sum: " + virtualSum.get());
        System.out.println("Java 21 virtual thread isVirtual: " + virtual.isVirtual());

        // Java 21: executor that creates one virtual thread per task.
        AtomicInteger executorSum = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                final int value = i;
                executor.submit(() -> executorSum.addAndGet(value));
            }
        }
        System.out.println("Java 21 virtual-thread executor sum: " + executorSum.get());
    }
}
