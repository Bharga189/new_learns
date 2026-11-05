package com.example.java16.comparison.streamcomparison;

import java.util.List;
import java.util.stream.Collectors;

/** Java 16 Stream Enhancements */
public class Java16StreamExample {

    public static void main(String[] args) {
        // Java 16: Stream operations with var
        var numbers = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        var sum = numbers.reduce(0, Integer::sum);
        System.out.println("Java 16 Stream.reduce: " + sum); // 15

        // Java 16: iterate with limit and toList
        var firstThree = java.util.stream.Stream.iterate(1, n -> n + 1)
                .limit(3)
                .toList();
        System.out.println("Java 16 iterate: " + firstThree); // [1, 2, 3]

        // Java 16: generate with var
        var random = java.util.stream.Stream.generate(Math::random)
                .limit(5)
                .toList();
        System.out.println("Java 16 generate: " + random);

        // Realtime usage: Advanced stream processing,
 // efficient data transformations, reactive programming patterns
    }
}