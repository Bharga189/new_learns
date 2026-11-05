package com.example.java14.comparison.streamcomparison;

import java.util.List;
import java.util.stream.Collectors;

/** Java 14 Stream Enhancements */
public class Java14StreamExample {

    public static void main(String[] args) {
        // Java 14: Stream of with var
        var numbers = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        var sum = numbers.reduce(0, Integer::sum);
        System.out.println("Java 14 Stream.reduce: " + sum); // 15

        // Java 14: iterate with limit and collect
        var firstThree = java.util.stream.Stream.iterate(1, n -> n + 1)
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Java 14 iterate: " + firstThree); // [1, 2, 3]
    }
}