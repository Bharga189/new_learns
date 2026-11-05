package com.example.java12.comparison.collectioncomparison;

/**
 * Java 12 Collection Enhancements
 * 
 * Java 12 added more methods to java.util.Collection and java.util.Map,
 * building on Java 9 immutable factories and Java 10/11 enhancements.
 * 
 * Java 12 New Collection/Map Methods:
 * 
 * 1. Map::computeIfAbsent - compute value if key absent (enhanced)
 * 2. Map::computeIfPresent - compute value if key present (enhanced)
 * 3. Collection::removeIf - already existed, enhanced with streams
 * 4. Map::remove - remove key mapping (Java 12 enhancement)
 * 
 * Java Version Progression:
 *   Java 9: List.of(), Set.of(), Map.of() - immutable collections
 *   Java 10: var keyword, collection improvements
 *   Java 11: isEmpty(), orElseThrow(), Optional.stream()
 *   Java 12: computeIfAbsent, computeIfPresent, Map.remove
 * 
 * Example: Map computeIfAbsent
 *   Map<String, Integer> scores = new HashMap<>();
 *   Integer score = scores.computeIfAbsent("Alice", k -> 0);
 *   // Returns 0 if key absent, otherwise existing value
 * 
 * Realtime usage: Map computation with lazy initialization,
 * collection filtering, building fluent map operations,
 * data processing workflows with null safety
 */
public class Java12CollectionExample {

    public static void main(String[] args) {
        // Java 11 style: manual compute if absent
        java.util.Map<String, Integer> scores11 = new java.util.HashMap<>();
        if (!scores11.containsKey("Alice")) {
            scores11.put("Alice", 0);
        }
        System.out.println("Java 11 manual: " + scores11);
        
        // Java 12: computeIfAbsent
        java.util.Map<String, Integer> scores12 = new java.util.HashMap<>();
        Integer aliceScore = scores12.computeIfAbsent("Alice", k -> k.length());
        System.out.println("Java 12 computeIfAbsent: " + aliceScore); // 0 (key was absent, lambda returns 0)
        
        // Add Alice later and recompute
        aliceScore = scores12.computeIfAbsent("Alice", k -> k.length() + 10);
        System.out.println("After update: " + aliceScore); // 10 (key now exists)
        
        // Realtime usage: Map computation with lazy initialization,
        // collection filtering, building fluent map operations,
        // data processing workflows with null safety
    }
}