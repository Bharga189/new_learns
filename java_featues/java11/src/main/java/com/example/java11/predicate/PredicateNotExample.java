package com.example.java11.predicate;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Java 11 Predicate.not()
 *
 * Negating a predicate used to require either a lambda (which repeats the
 * predicate body) or Predicate.negate(). Java 11 added the static not()
 * factory, which reads well with method references.
 *
 * Java 8-10:
 *   names.stream().filter(name -> !name.isBlank())
 *
 * Java 11:
 *   Predicate<String> blank = String::isBlank;
 *   names.stream().filter(Predicate.not(blank))
 */
public class PredicateNotExample {

    public static void main(String[] args) {
        List<String> names = List.of("Alice", " ", "Bob", "", "Carol");

        // Java 8-10 style: negation inside a lambda
        List<String> nonBlankOld = names.stream()
                .filter(name -> !name.isBlank())
                .collect(Collectors.toList());
        System.out.println("Java 8-10 style: " + nonBlankOld);

        // Java 11: Predicate.not() keeps the method reference readable
        Predicate<String> blank = String::isBlank;
        List<String> nonBlankNew = names.stream()
                .filter(Predicate.not(blank))
                .collect(Collectors.toList());
        System.out.println("Java 11 Predicate.not: " + nonBlankNew);
    }
}
