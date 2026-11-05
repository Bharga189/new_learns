package com.example.java15.comparison.streamcomparison;

import java.util.List;
import java.util.stream.Collectors;

/** Java 15 Stream Enhancements */
public class Java15StreamExample {

    public static void main(String[] args) {
        // Java 15: Stream operations with var
        var numbers = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        var sum = numbers.reduce(0, Integer::sum);
        System.out.println("Java 15 Stream.reduce: " + sum); // 15

        // Java 15: iterate with limit and collect
        var firstThree = java.util.stream.Stream.iterate(1, n -> n + 1)
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Java 15 iterate: " + firstThree); // [1, 2, 3]

        // Java 15: generate with var
        var random = java.util.stream.Stream.generate(Math::random)
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Java 15 generate: " + random);

        // Realtime usage: Advanced stream processing,
 // efficient data transformations, reactive programming patterns
    }
}