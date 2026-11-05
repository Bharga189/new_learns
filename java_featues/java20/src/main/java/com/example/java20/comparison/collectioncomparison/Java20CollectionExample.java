package com.example.java20.comparison.collectioncomparison;

import java.util.HashMap;
import java.util.Map;

/** Java 20 Collection Enhancements */
public class Java20CollectionExample {

    public static void main(String[] args) {
        // Java 20: Map operations with var
        var scores = new HashMap<String, Integer>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        System.out.println("Java 20 HashMap: " + scores);

        // computeIfAbsent - compute if key absent
        var aliceScore = scores.computeIfAbsent("Alice", k -> k.length());
        System.out.println("computeIfAbsent for Alice: " + aliceScore);

        // computeIfPresent - compute if key exists
        var bobScore = scores.computeIfPresent("Bob", (k, v) -> v + 10);
        System.out.println("computeIfPresent for Bob: " + bobScore);

        // After operations
        System.out.println("After: " + scores);

        // Realtime usage: Map computation with lazy initialization,
 // collection filtering, building fluent map operations
    }
}