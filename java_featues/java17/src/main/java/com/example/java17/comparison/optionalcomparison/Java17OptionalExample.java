package com.example.java17.comparison.optionalcomparison;

import java.util.Optional;

/** Java 17 Optional Enhancements */
public class Java17OptionalExample {

    public static void main(String[] args) {
        // Java 17: Optional operations
        var opt = java.util.Optional.of("Java 17 Optional");

        // isEmpty check
        boolean empty = opt.isEmpty();
        System.out.println("Optional isEmpty: " + empty);

        // ifPresentOrElse
        opt.ifPresentOrElse(
            n -> System.out.println("Value: " + n),
            () -> System.out.println("Optional is empty")
        );

        // stream()
        var stream = opt.stream();
        System.out.println("Optional.stream(): " + stream.count());

        // Realtime usage: Converting optionals to streams,
 // filtering compositions, functional data processing
    }
}