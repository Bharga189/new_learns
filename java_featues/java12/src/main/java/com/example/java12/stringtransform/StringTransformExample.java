package com.example.java12.stringtransform;

/**
 * Java 12 String.indent() and String.transform()
 *
 * indent(n) adds n spaces to the start of every line and normalizes line
 * terminators. transform(f) applies a function to the string, which allows
 * fluent chains without intermediate variables.
 *
 * Java 11 style:
 *   String padded = "    " + text.replace("\n", "\n    ");
 *
 * Java 12:
 *   String padded = text.indent(4);
 *   String upper = "  hello  ".transform(String::strip).transform(String::toUpperCase);
 */
public class StringTransformExample {

    public static void main(String[] args) {
        String text = "line1\nline2";

        // Java 11 style: manual indentation
        String manual = "    " + text.replace("\n", "\n    ");
        System.out.println("Manual indentation:\n" + manual);

        // Java 12: indent() adds spaces to every line
        System.out.println("indent(4):\n" + text.indent(4));

        // Java 12: transform() applies a function and returns its result
        String upper = "  hello  ".transform(String::strip).transform(String::toUpperCase);
        System.out.println("transform: '" + upper + "'");
    }
}
