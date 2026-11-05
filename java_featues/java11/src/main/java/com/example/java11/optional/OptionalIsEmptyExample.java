package com.example.java11.optional;

import java.util.Optional;

/**
 * Java 11 Optional.isEmpty()
 *
 * Before Java 11, checking for an absent value required negating isPresent(),
 * which reads awkwardly. Java 11 added isEmpty() as the direct counterpart.
 *
 * Java 8-10:
 *   if (!optional.isPresent()) { ... }
 *
 * Java 11:
 *   if (optional.isEmpty()) { ... }
 *
 * Both methods are exact opposites; use whichever reads better at the call site.
 */
public class OptionalIsEmptyExample {

    public static void main(String[] args) {
        Optional<String> present = Optional.of("value");
        Optional<String> empty = Optional.empty();

        // Java 8-10 style: negation of isPresent()
        System.out.println("!isPresent (present): " + !present.isPresent()); // false
        System.out.println("!isPresent (empty): " + !empty.isPresent());     // true

        // Java 11: isEmpty()
        System.out.println("isEmpty (present): " + present.isEmpty()); // false
        System.out.println("isEmpty (empty): " + empty.isEmpty());     // true
    }
}
