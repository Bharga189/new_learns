package com.example.java21.sequencedcollections;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Java 21: Sequenced Collections (JEP 431)
 *
 * <p>Java 20: collections with a defined encounter order (such as {@link List},
 * {@link java.util.Deque}, {@link java.util.SortedSet}, and
 * {@link java.util.LinkedHashSet}) exposed first/last access in different ways.
 * Getting the last element of a {@code List} typically used
 * {@code list.get(list.size() - 1)}, and there was no common type for "a
 * collection with first/last elements and a reverse view".
 *
 * <p>Java 21: {@link SequencedCollection}, {@link java.util.SequencedSet}, and
 * {@link java.util.SequencedMap} provide a uniform API: {@code getFirst()},
 * {@code getLast()}, {@code addFirst()}, {@code addLast()},
 * {@code removeFirst()}, {@code removeLast()}, and {@code reversed()}.
 */
public class SequencedCollectionsExample {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("Alice", "Bob", "Carol"));

        // Java 20 style: index-based or collection-specific access.
        String java20First = names.get(0);
        String java20Last = names.get(names.size() - 1);
        System.out.println("Java 20 style first/last: " + java20First + ", " + java20Last);

        // Java 21: uniform sequenced operations.
        SequencedCollection<String> sequenced = names;
        sequenced.addFirst("Zoe");
        sequenced.addLast("Dave");
        System.out.println("Java 21 getFirst/getLast: " + sequenced.getFirst() + ", " + sequenced.getLast());
        System.out.println("Java 21 reversed view: " + sequenced.reversed());

        LinkedHashSet<Integer> numbers = new LinkedHashSet<>(List.of(10, 20, 30));
        System.out.println("Java 21 SequencedSet first: " + numbers.getFirst());
        System.out.println("Java 21 SequencedSet reversed: " + numbers.reversed());
    }
}
