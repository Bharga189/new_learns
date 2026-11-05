package com.example.java15.comparison.optionalcomparison;

import java.util.Optional;

/** Java 15 Optional Enhancements */
public class Java15OptionalExample {

    public static void main(String[] args) {
        // Java 15: Optional operations
        var opt = java.util.Optional.of("Java 15 Optional");

        // isEmpty check
        boolean empty = opt.isEmpty();
        System.out.println("Optional isEmpty: " + empty); // false

        // ifPresentOrElse
        opt.ifPresentOrElse(
            n -> System.out.println("Value: " + n),
            () -> System.out.println("Optional is empty")
        );

        // stream()
        var stream = opt.stream();
        System.out.println("Optional.stream(): " + stream.count()); // 1

        // Realtime usage: Converting optionals to streams,
 // filtering compositions, functional data processing pipelines
    }
}