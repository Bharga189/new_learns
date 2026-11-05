package com.example.java13.comparison.optionalcomparison;

import java.util.Optional;

/** Java 13 Optional Enhancements */
public class Java13OptionalExample {

    public static void main(String[] args) {
        // Java 13: Optional operations
        var opt = java.util.Optional.of("Java 13 Optional");
        
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