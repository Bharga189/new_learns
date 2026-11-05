package com.example.java12.comparison.optionalcomparison;

import java.util.Optional;

/**
 * Java 12 Optional Enhancements
 */
public class Java12OptionalExample {

    public static void main(String[] args) {
        // Java 12: Optional operations
        var opt = java.util.Optional.of("Java 12 Optional");
        
        // isEmpty check
        boolean empty = opt.isEmpty();
        System.out.println("Optional isEmpty: " + empty); // false
        
        // ifPresentOrElse (Java 9 carry-forward)
        opt.ifPresentOrElse(
            n -> System.out.println("Value: " + n),
            () -> System.out.println("Optional is empty")
        );
        
        // stream() (Java 11 carry-forward)
        var stream = opt.stream();
        System.out.println("Optional.stream(): " + stream.count()); // 1
    }
}