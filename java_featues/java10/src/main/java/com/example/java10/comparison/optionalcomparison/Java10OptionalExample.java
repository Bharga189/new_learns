package com.example.java10.comparison.optionalcomparison;

/**
 * Java 10 Optional Enhancements
 * 
 * Java 10 added new methods to the Optional class,
 * building on the Java 9 ifPresentOrElse addition.
 * 
 * Java 8 Optional methods:
 * - ofNullable(), ifPresent(), orElse(), orElseGet(),
 *   filter(), map(), flatMap()
 * 
 * Java 9 added:
 * - ifPresentOrElse(Consumer, Runnable)
 * - stream()
 * - or(Supplier)
 * 
 * Java 10 added:
 * - orElseThrow() with supplier overload
 *   - orElseThrow(() -> new NoSuchElementException())
 *   - orElseThrow(Supplier<? extends Exception>)
 * - Additional utility methods
 * 
 * Note: Optional.isEmpty() was added in Java 11; use !isPresent() here.
 * 
 * Example: Java 10 Optional methods
 *   Optional<String> opt = Optional.of("Hello");
 *   boolean present = opt.isPresent(); // true
 *   String value = opt.orElseThrow(() -> new RuntimeException("Missing"));
 * 
 * Benefits:
 * - More expressive null handling
 * - Reduced boilerplate for exception throwing
 * - Better API consistency
 * 
 * Realtime usage: Safe null handling with expressive error messages,
 * required value validation, API design with clear failure semantics
 */
public class Java10OptionalExample {

    public static void main(String[] args) {
        // Java 9 style
        String name = null;
        java.util.Optional<String> opt9 = java.util.Optional.ofNullable(name);
        opt9.ifPresentOrElse(
            n -> System.out.println("Java 9: Name present: " + n),
            () -> System.out.println("Java 9: Name is empty")
        );
        
        // Java 10 enhancements
        var opt10 = java.util.Optional.of("Java 10 Optional");
        boolean isPresent = opt10.isPresent();
        System.out.println("Java 10 isPresent: " + isPresent); // true
        
        // orElseThrow with supplier
        String value = opt10.orElseThrow(() -> new java.lang.RuntimeException("No value"));
        System.out.println("Java 10 orElseThrow: " + value);
        
        // Realtime usage: Safe null handling with expressive error messages,
        // required value validation, API design with clear failure semantics
    }
}