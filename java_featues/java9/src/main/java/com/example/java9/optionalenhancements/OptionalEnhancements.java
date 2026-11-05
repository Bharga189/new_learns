package com.example.java9.optionalenhancements;

import java.util.Optional;

/**
 * Java 9 Optional Enhancements Example
 * 
 * Java 9 added new methods to the Optional class to provide more expressive
 * null-handling capabilities beyond what was available in Java 8.
 * 
 * Java 8 Optional methods:
 * - ofNullable(), ifPresent(), orElse(), orElseGet(), filter(), map()
 * 
 * Java 9 New Optional methods:
 * - ifPresentOrElse(Consumer<? super T> action, Runnable empty) - 
 *   performs action if present, runs empty action if absent
 * - stream() - returns a Stream Optional, useful for stream operations
 * - or(Supplier<? extends Optional<? extends T>> supplier) - 
 *   alternative to isPresent() ? get() : supplier.get()
 * 
 * Example usage of ifPresentOrElse:
 * optional.ifPresentOrElse(
 *     n -> System.out.println("Name: " + n),
 *     () -> System.out.println("Name is null")
 * );
 * 
 * Realtime usage: Safe null handling with alternative actions,
 * UI text display when data is missing, logging with fallback messages
 */
public class OptionalEnhancements {

    public static void main(String[] args) {
        String name = null;
        Optional<String> optional = Optional.ofNullable(name);
        
        // Java 9: ifPresentOrElse - new method for expressive null handling
        optional.ifPresentOrElse(
            n -> System.out.println("Name: " + n),
            () -> System.out.println("Name is null")
        );
        
        // Alternative: traditional approach (Java 8 style)
        // if (optional.isPresent()) {
        //     System.out.println("Name: " + optional.get());
        // } else {
        //     System.out.println("Name is null");
        // }
        
        // Realtime usage: Safe null handling with alternative actions,
        // UI text display when data is missing, logging with fallback messages
    }
}