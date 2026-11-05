package com.example.java16.patternmatching;

/** Java 16 instanceof Pattern (Enhanced) */
public class InstanceofPatternExample {

    public static void main(String[] args) {
        // Java 16 instanceof pattern - enhanced null checking
        Object obj = "Hello World";
        if (obj instanceof String s) {
            System.out.println("Java 16 instanceof pattern - length: " + s.length());
            System.out.println("Java 16 instanceof pattern - uppercase: " + s.toUpperCase());
        }

        // Null safety - instanceof returns false for null
        Object nullObj = null;
        boolean isString = nullObj instanceof String;
        System.out.println("Null check: " + isString); // false

        // Chained instanceof with pattern matching
        Object obj2 = 42;
        if (obj2 instanceof Integer i && i > 0) {
            System.out.println("Positive integer: " + i);
        }

        // Multiple patterns in instanceof
        Object obj3 = "test";
        if (obj3 instanceof String s && s.length() > 0) {
            System.out.println("Non-empty string: " + s);
        }

        // instanceof with var
        Object obj4 = "pattern var";
        if (obj4 instanceof String var) {
            System.out.println("String var length: " + var.length());
        }
    }
}