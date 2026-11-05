package com.example.java19.comparison.collectioncomparison;

import java.util.HashMap;
import java.util.Map;

/** Java 19 Collection Enhancements */
public class Java19CollectionExample {

    public static void main(String[] args) {
        // Java 19: Map operations with var
        var scores = new HashMap<String, Integer>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        System.out.println("Java 19 HashMap: " + scores);

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