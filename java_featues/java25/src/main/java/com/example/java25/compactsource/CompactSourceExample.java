package com.example.java25.compactsource;

import java.util.List;

/**
 * Java 25: Compact Source Files and Instance Main Methods (JEP 512)
 *
 * <p>Java 24: a compiled Java program normally declared an explicit class and a
 * {@code public static void main(String[] args)} entry point. Even small
 * programs carried that ceremony, and the source launcher still centered on the
 * traditional main method form.
 *
 * <p>Java 25: a source file can be compact. It may omit an explicit class
 * declaration, and the launched entry point can be an instance {@code main}
 * method with simplified signatures such as {@code void main()}. The Java
 * runtime creates the instance and invokes {@code main}. This feature is most
 * visible when launching source files directly, but the underlying language
 * support is final in Java 25.
 *
 * <p>Try a compact source file with Java 25:
 * <pre>{@code
 * void main() {
 *     IO.println("Hello from a compact source file");
 * }
 * }</pre>
 *
 * <p>Save as {@code Hello.java} and run: {@code java Hello.java}
 *
 * <p>This compiled example keeps an explicit class so it fits the repository's
 * packaged/module layout, while printing the Java 24 vs Java 25 forms.
 */
public class CompactSourceExample {

    public static void main(String[] args) {
        String java24Form = String.join("\n",
                "public class Hello {",
                "    public static void main(String[] args) {",
                "        System.out.println(\"Hello, Java 24\");",
                "    }",
                "}");
        System.out.println("Java 24 style (explicit class + static main):");
        System.out.println(java24Form);

        String java25Form = String.join("\n",
                "void main() {",
                "    IO.println(\"Hello, Java 25\");",
                "}");
        System.out.println();
        System.out.println("Java 25 style (compact source file + instance main, JEP 512):");
        System.out.println(java25Form);

        List<String> differences = List.of(
                "explicit class declaration can be omitted in a compact source file",
                "entry point can be an instance main method",
                "simple programs need less ceremony");
        System.out.println();
        System.out.println("Key differences: " + differences);
    }
}
