package com.example.java9.streamenhancements;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 9 Stream Enhancements Example
 * 
 * Java 9 added two new stream operations that enable early termination
 * and conditional processing, which were not available in Java 8.
 * 
 * Java 8 Stream operations:
 * - filter(), map(), limit(), skip() - for transformation and filtering
 * - terminal operations: collect(), forEach(), reduce(), etc.
 * - No built-in way to stop processing early based on content
 * 
 * Java 9 New Stream operations:
 * - takeWhile(Predicate<? super T> predicate) - 
 *   returns a stream consisting of the longest prefix of elements 
 *   that match the given predicate
 * - dropWhile(Predicate<? super T> predicate) - 
 *   returns a stream consisting of the remaining elements after 
 *   the longest prefix of elements matching the predicate are dropped
 * 
 * takeWhile example:
 * words.stream().takeWhile(w -> w.length() < 5)
 * - Returns: "java" (stops before "python" which is >= 5 chars)
 * 
 * dropWhile example:
 * words.stream().dropWhile(w -> w.length() < 5)
 * - Drops: "java", returns: "python", "go", "rust", "kotlin"
 * 
 * Realtime usage: Processing data streams with early termination,
 * reading config files until comment line, filtering logs,
 * conditional stream processing, early exit scenarios
 */
public class StreamEnhancements {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("java", "python", "go", "rust", "kotlin");
        
        // Java 9: takeWhile - stops when predicate fails
        // Returns elements while predicate is true, stops at first false
        List<String> takeResult = words.stream()
                .takeWhile(w -> w.length() < 5)
                .collect(Collectors.toList());
        System.out.println("takeWhile (length < 5): " + takeResult);
        // Output: [java] - stops at "python" (length 6 >= 5)
        
        // Java 9: dropWhile - drops elements while predicate matches
        // Drops elements while predicate is true, returns remainder
        List<String> dropResult = words.stream()
                .dropWhile(w -> w.length() < 5)
                .collect(Collectors.toList());
        System.out.println("dropWhile (length < 5): " + dropResult);
        // Output: [python, go, rust, kotlin] - drops "java", returns rest
        
        // Realtime usage: Processing data streams with early termination,
        // reading config files until comment line, filtering logs,
        // conditional stream processing, early exit scenarios
    }
}