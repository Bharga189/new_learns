package com.example.java21.switchpatterns;

/**
 * Java 21: Pattern Matching for switch (JEP 441)
 *
 * <p>Java 20: {@code switch} could select on primitives, strings, and enums,
 * and Java 14 added switch expressions. But switching over an arbitrary object
 * type usually meant an {@code if-else instanceof} chain with casts or pattern
 * variables, and {@code null} required a separate check.
 *
 * <p>Java 21: {@code switch} supports type patterns, guarded patterns with
 * {@code when}, and a {@code null} case. The compiler checks pattern
 * dominance/order, and switch expressions can return values directly.
 */
public class PatternMatchingSwitchExample {

    public static void main(String[] args) {
        System.out.println("Java 20 style (if-else chain): " + describeJava20Style("hello"));
        System.out.println("Java 21 switch pattern: " + describe("hello"));
        System.out.println("Java 21 switch pattern: " + describe(42));
        System.out.println("Java 21 switch pattern: " + describe(0));
        System.out.println("Java 21 switch pattern: " + describe(3.14));
        System.out.println("Java 21 switch pattern: " + describe(null));
    }

    static String describeJava20Style(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String s) {
            return "String of length " + s.length();
        } else if (obj instanceof Integer i) {
            return "Integer " + i;
        } else {
            return "Something else: " + obj;
        }
    }

    static String describe(Object obj) {
        return switch (obj) {
            case null -> "null";
            case String s when s.isBlank() -> "blank String";
            case String s -> "String of length " + s.length();
            case Integer i when i == 0 -> "Integer zero";
            case Integer i -> "Integer " + i;
            case Double d -> "Double " + d;
            default -> "Something else: " + obj;
        };
    }
}
