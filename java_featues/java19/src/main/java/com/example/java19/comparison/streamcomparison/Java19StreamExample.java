package com.example.java19.comparison.streamcomparison;

import java.util.List;
import java.util.stream.Collectors;

/** Java 19 Stream Enhancements */
public class Java19StreamExample {

    public static void main(String[] args) {
        // Java 19: Stream operations with var
        var numbers = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        var sum = numbers.reduce(0, Integer::sum);
        System.out.println("Java 19 Stream.reduce: " + sum);

        // Java 19: iterate with limit and toList
        var firstThree = java.util.stream.Stream.iterate(1, n -> n + 1)
                .limit(3)
                .toList();
        System.out.println("Java 19 iterate: " + firstThree);

        // Java 19: generate with var
        var random = java.util.stream.Stream.generate(Math::random)
                .limit(5)
                .toList();
        System.out.println("Java 19 generate: " + random);

        // Realtime usage: Advanced stream processing,
 // efficient data transformations, reactive programming
    }
}