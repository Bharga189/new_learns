package com.example.java24.streamgatherers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;

/**
 * Java 24: Stream Gatherers (JEP 485)
 *
 * <p>Java 23: the Stream API had fixed intermediate operations such as
 * {@code map}, {@code filter}, and {@code flatMap}. Custom stream
 * transformations usually meant collecting to an intermediate structure and
 * streaming again, using {@code mapMulti}, or writing specialized collectors.
 * Stream Gatherers existed only as a preview API, so this repository's
 * {@code --release 23} (no {@code --enable-preview}) code could not use them.
 *
 * <p>Java 24: {@code Stream.gather(Gatherer)} is final. A gatherer can
 * transform elements, track state, and push zero or more elements downstream.
 * {@link java.util.stream.Gatherers} provides built-in gatherers such as
 * {@code windowFixed}, {@code windowSliding}, {@code fold}, and {@code scan}.
 */
public class StreamGatherersExample {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);

        // Java 23 style: fixed-size windows by collecting/indexing manually.
        List<List<Integer>> java23Windows = fixedWindowsWithoutGatherer(numbers, 3);
        System.out.println("Java 23 windows (manual): " + java23Windows);

        // Java 24: Gatherers.windowFixed creates fixed-size groups.
        List<List<Integer>> java24Windows = numbers.stream()
                .gather(Gatherers.windowFixed(3))
                .toList();
        System.out.println("Java 24 Gatherers.windowFixed(3): " + java24Windows);

        // Java 23 style: running totals by accumulating outside the stream.
        List<Integer> java23RunningTotals = runningTotalsWithoutGatherer(numbers);
        System.out.println("Java 23 running totals (manual): " + java23RunningTotals);

        // Java 24: Gatherers.scan emits each intermediate result.
        List<Integer> java24RunningTotals = numbers.stream()
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();
        System.out.println("Java 24 Gatherers.scan(sum): " + java24RunningTotals);

        // Java 24: Gatherers.fold emits one final result.
        Integer product = IntStream.of(1, 2, 3, 4)
                .boxed()
                .gather(Gatherers.fold(() -> 1, (acc, value) -> acc * value))
                .findFirst()
                .orElse(0);
        System.out.println("Java 24 Gatherers.fold(product): " + product);
    }

    private static <T> List<List<T>> fixedWindowsWithoutGatherer(List<T> values, int size) {
        List<List<T>> windows = new ArrayList<>();
        for (int i = 0; i < values.size(); i += size) {
            windows.add(new ArrayList<>(values.subList(i, Math.min(i + size, values.size()))));
        }
        return windows;
    }

    private static List<Integer> runningTotalsWithoutGatherer(List<Integer> values) {
        List<Integer> totals = new ArrayList<>();
        AtomicInteger sum = new AtomicInteger();
        for (Integer value : values) {
            totals.add(sum.addAndGet(value));
        }
        return totals;
    }
}
