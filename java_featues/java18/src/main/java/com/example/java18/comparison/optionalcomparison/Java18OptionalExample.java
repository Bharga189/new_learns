package com.example.java18.comparison.optionalcomparison;

import java.util.Optional;

/** Java 18 Optional Enhancements */
public class Java18OptionalExample {

    public static void main(String[] args) {
        // Java 18: Optional operations
        var opt = java.util.Optional.of("Java 18 Optional");

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