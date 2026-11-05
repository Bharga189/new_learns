package com.example.java22.unnamedvariables;

import java.io.StringReader;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Java 22: Unnamed Variables & Patterns (JEP 456)
 *
 * Java 21: every variable needed a name, even when its value was never used,
 * which led to names like "ignored" or "unused". Record patterns also had to
 * bind every component, even the ones the code did not need.
 *
 * Java 22: the underscore declares an unnamed variable or pattern. It binds no
 * name, so there is nothing to read and no unused-variable warning; several
 * underscores can appear in the same scope.
 *
 * Unnamed variables may be declared in:
 *   - local variable declarations
 *   - catch parameters
 *   - for-each loops
 *   - try-with-resources
 *   - lambda parameters
 *
 * Unnamed patterns may appear in:
 *   - record patterns (instanceof and switch)
 *
 * Preview in Java 21 (JEP 443), final in Java 22.
 */
public class UnnamedVariablesExample {

    record Point(int x, int y) {
    }

    public static void main(String[] args) {
        // Java 21 style: an unused value still needs a descriptive name
        int ignored = Integer.parseInt("42");
        System.out.println("Java 21 style parsed: " + ignored);

        // Java 22: unnamed local variable
        var _ = Integer.parseInt("42");
        System.out.println("Java 22 unnamed local variable declared");

        // Java 21 style: unused loop variable
        List<String> names = List.of("Alice", "Bob", "Carol");
        int java21Count = 0;
        for (String name : names) {
            java21Count++;
        }
        System.out.println("Java 21 style count: " + java21Count);

        // Java 22: unnamed for-each variable
        int java22Count = 0;
        for (var _ : names) {
            java22Count++;
        }
        System.out.println("Java 22 unnamed for-each count: " + java22Count);

        // Java 22: unnamed catch parameter
        try {
            Integer.parseInt("not a number");
        } catch (NumberFormatException _) {
            System.out.println("Java 22 unnamed catch parameter");
        }

        // Java 22: unnamed resource in try-with-resources
        try (var _ = new StringReader("data")) {
            System.out.println("Java 22 unnamed try-with-resources variable");
        }

        // Java 22: unnamed lambda parameter
        BiFunction<String, String, String> first = (value, _) -> value;
        System.out.println("Java 22 unnamed lambda parameter: " + first.apply("left", "right"));

        // Java 21 style: a record pattern binds every component
        Object shape = new Point(3, 4);
        if (shape instanceof Point(int x, int y)) {
            System.out.println("Java 21 style record pattern x: " + x + ", y: " + y);
        }

        // Java 22: unnamed pattern for the component that is not needed
        if (shape instanceof Point(int x, int _)) {
            System.out.println("Java 22 unnamed pattern x: " + x);
        }
    }
}
