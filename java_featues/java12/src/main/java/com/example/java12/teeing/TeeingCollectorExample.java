package com.example.java12.teeing;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 12 Collectors.teeing()
 *
 * teeing() sends every element to two downstream collectors and merges their
 * results with a BiFunction, so one pass over the data can compute two
 * aggregates at once.
 *
 * Java 8-11: two separate stream passes
 *   long count = numbers.stream().count();
 *   int sum = numbers.stream().mapToInt(Integer::intValue).sum();
 *
 * Java 12: one pass
 *   numbers.stream().collect(Collectors.teeing(
 *       Collectors.counting(),
 *       Collectors.summingInt(Integer::intValue),
 *       (n, total) -> ...));
 */
public class TeeingCollectorExample {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(3, 7, 2, 9, 4, 1);

        // Java 8-11 style: two passes over the same data
        long count = numbers.stream().count();
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Two passes: count=" + count + ", sum=" + sum);

        // Java 12: both collectors in a single pass
        String summary = numbers.stream().collect(Collectors.teeing(
                Collectors.counting(),
                Collectors.summingInt(Integer::intValue),
                (n, total) -> "count=" + n + ", sum=" + total + ", average=" + (total / (double) n)));
        System.out.println("teeing: " + summary);
    }
}
