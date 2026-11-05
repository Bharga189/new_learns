package com.example.java25.flexibleconstructors;

/**
 * Java 25: Flexible Constructor Bodies (JEP 513)
 *
 * <p>Java 24: an explicit constructor invocation ({@code super(...)} or
 * {@code this(...)}) had to be the first statement in a constructor. Any
 * validation or argument preparation had to be moved into static helper
 * methods, auxiliary constructors, or repeated at the call site.
 *
 * <p>Java 25: a constructor body may contain a <em>prologue</em> — statements
 * that run before the explicit constructor invocation. The prologue may
 * validate arguments and compute values, but it must not read uninitialized
 * fields of the instance being created or use {@code this}/{@code super} as an
 * expression. After {@code super(...)} or {@code this(...)}, the constructor
 * continues normally.
 */
public class FlexibleConstructorsExample {

    static class Java24StyleRange {
        private final int start;
        private final int end;

        Java24StyleRange(int start, int end) {
            // Java 24 style: super() must be first; validation is delegated.
            super();
            validate(start, end);
            this.start = start;
            this.end = end;
        }

        private static void validate(int start, int end) {
            if (start > end) {
                throw new IllegalArgumentException("start must be <= end");
            }
        }

        int length() {
            return end - start;
        }
    }

    static class Java25StyleRange {
        private final int start;
        private final int end;

        Java25StyleRange(int start, int end) {
            // Java 25: prologue runs before the explicit constructor invocation.
            if (start > end) {
                throw new IllegalArgumentException("start must be <= end");
            }
            int normalizedStart = Math.max(0, start);
            super();
            this.start = normalizedStart;
            this.end = end;
        }

        int length() {
            return end - start;
        }
    }

    public static void main(String[] args) {
        Java24StyleRange java24 = new Java24StyleRange(2, 9);
        System.out.println("Java 24 style range length: " + java24.length());

        Java25StyleRange java25 = new Java25StyleRange(-3, 9);
        System.out.println("Java 25 style range length (normalized start): " + java25.length());

        try {
            new Java25StyleRange(10, 1);
        } catch (IllegalArgumentException e) {
            System.out.println("Java 25 prologue validation: " + e.getMessage());
        }
    }
}
