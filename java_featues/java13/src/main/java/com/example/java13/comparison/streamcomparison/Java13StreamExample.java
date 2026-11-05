package com.example.java13.comparison.streamcomparison;

import java.util.List;
import java.util.stream.Collectors;

/** Java 13 Stream Enhancements */
public class Java13StreamExample {

    public static void main(String[] args) {
        // Java 13: Stream iterate with var
        var firstFiveEven = java.util.stream.Stream.iterate(0, n -> n + 2)
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Java 13 iterate: " + firstFiveEven); // [0, 2, 4, 6, 8]
        
        // Java 13: Stream generate
        var randomStrings = java.util.stream.Stream.generate(() -> "item")
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Java 13 generate: " + randomStrings); // [item, item, item]
    }
}