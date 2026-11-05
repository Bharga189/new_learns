package com.example.java14.comparison.optionalcomparison;

import java.util.Optional;

/** Java 14 Optional Enhancements */
public class Java14OptionalExample {

    public static void main(String[] args) {
        // Java 14: Optional operations
        var opt = java.util.Optional.of("Java 14 Optional");

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
    }
}