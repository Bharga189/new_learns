package com.example.java13.comparison.collectioncomparison;

import java.util.HashMap;
import java.util.Map;

/** Java 13 Collection Enhancements */
public class Java13CollectionExample {

    public static void main(String[] args) {
        // Java 13: Map computeIfAbsent with mutable map
        var scores = new java.util.HashMap<String, Integer>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        System.out.println("Java 13 HashMap: " + scores);
        
        // computeIfAbsent - compute new value if key absent
        var aliceScore = scores.computeIfAbsent("Alice", k -> k.length());
        System.out.println("computeIfAbsent for Alice: " + aliceScore); // 5 (length of "Alice")
        
        // computeIfPresent - compute new value if key exists
        var bobScore = scores.computeIfPresent("Bob", (k, v) -> v + 10);
        System.out.println("computeIfPresent for Bob: " + bobScore); // 97 (87 + 10)
        
        // After computeIfAbsent, Alice's value is updated
        System.out.println("After computeIfAbsent: " + scores);
        
        // After computeIfPresent, Bob's value is updated
        System.out.println("After computeIfPresent: " + scores);
    }
}