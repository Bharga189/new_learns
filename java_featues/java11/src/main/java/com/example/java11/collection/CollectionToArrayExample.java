package com.example.java11.collection;

import java.util.Arrays;
import java.util.List;

/**
 * Java 11 Collection.toArray(IntFunction)
 *
 * Java 11 added an overload that takes a generator for the result array,
 * replacing the old idiom of passing a zero-length array (or sizing it
 * manually) to Collection.toArray(T[]).
 *
 * Java 8-10:
 *   String[] array = list.toArray(new String[list.size()]);
 *
 * Java 11:
 *   String[] array = list.toArray(String[]::new);
 */
public class CollectionToArrayExample {

    public static void main(String[] args) {
        List<String> names = List.of("Alice", "Bob", "Carol");

        // Java 8-10 style: allocate the array yourself
        String[] oldWay = names.toArray(new String[names.size()]);
        System.out.println("Java 8-10 style: " + Arrays.toString(oldWay));

        // Java 11: the collection creates an array of the requested type
        String[] newWay = names.toArray(String[]::new);
        System.out.println("Java 11 toArray: " + Arrays.toString(newWay));
    }
}
