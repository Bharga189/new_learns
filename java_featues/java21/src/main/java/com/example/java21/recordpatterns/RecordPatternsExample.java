package com.example.java21.recordpatterns;

/**
 * Java 21: Record Patterns (JEP 440)
 *
 * <p>Java 20: pattern matching for {@code instanceof} (final since Java 16) let
 * code test a type and bind a variable, but deconstructing a record still
 * required a cast/check followed by explicit accessor calls such as
 * {@code point.x()} and {@code point.y()}.
 *
 * <p>Java 21: record patterns deconstruct record values directly. In
 * {@code if (obj instanceof Point(int x, int y))}, the components are matched
 * and bound in one step. Nested record patterns can also deconstruct components
 * that are themselves records.
 */
public class RecordPatternsExample {

    record Point(int x, int y) {
    }

    record Line(Point start, Point end) {
    }

    public static void main(String[] args) {
        Object shape = new Point(3, 4);

        // Java 20 style: instanceof + explicit accessors.
        if (shape instanceof Point p) {
            System.out.println("Java 20 style accessors: x=" + p.x() + ", y=" + p.y());
        }

        // Java 21: record pattern deconstructs components.
        if (shape instanceof Point(int x, int y)) {
            System.out.println("Java 21 record pattern: x=" + x + ", y=" + y);
        }

        Object value = new Line(new Point(0, 0), new Point(5, 5));
        if (value instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
            int dx = x2 - x1;
            int dy = y2 - y1;
            System.out.println("Java 21 nested record pattern: dx=" + dx + ", dy=" + dy);
        }

        System.out.println("Java 21 formatted point: " + format(shape));
    }

    static String format(Object obj) {
        // Record patterns work in pattern contexts; here we keep the example simple.
        if (obj instanceof Point(int x, int y)) {
            return "(" + x + ", " + y + ")";
        }
        return String.valueOf(obj);
    }
}
