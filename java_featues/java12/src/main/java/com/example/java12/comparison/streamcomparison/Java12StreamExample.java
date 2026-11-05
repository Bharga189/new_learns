package com.example.java12.comparison.streamcomparison;

import java.util.stream.Stream;

/**
 * Java 12 Stream Enhancements
 */
public class Java12StreamExample {

    public static void main(String[] args) {
        // Java 12: Stream.of with var
        var numbers = Stream.of(1, 2, 3, 4, 5);
        var sum = numbers.reduce(0, Integer::sum);
        System.out.println("Java 12 Stream.reduce: " + sum); // 15
        
        // Java 12: iterate with var
        var firstTenOdd = Stream.iterate(1, n -> n + 2)
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Java 12 iterate: " + firstTenOdd); // [1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
    }
}