package com.example.java23.markdowndoc;

import java.util.List;

/**
 * Java 23: Markdown Documentation Comments (JEP 467)
 *
 * <p>Java 22: API documentation was written with traditional Javadoc comments.
 * Formatting relied on HTML tags such as {@code <p>}, {@code <pre>},
 * {@code <ul>/<li>}, and {@code {@code ...}}. This works well, but common
 * constructs like paragraphs, lists, and code blocks are verbose to read and
 * write in source.</p>
 *
 * <p>Java 23: documentation comments may also start with {@code ///}. In a
 * {@code ///} comment, the body is Markdown (CommonMark), so paragraphs are
 * blank lines, lists use {@code -} or {@code *}, emphasis uses
 * {@code *...*}/{@code **...**}, and fenced code blocks use triple backticks.
 * Standard Javadoc tags such as {@code @param}, {@code @return}, and
 * {@code @throws} still work. Traditional {@code /** ... *}{@code /} comments
 * remain fully supported.</p>
 *
 * <p>Compile or generate docs with {@code --release 23} / a Java 23 toolchain.
 * This example runs the same logic regardless of comment style; it prints a
 * side-by-side summary of the two documentation forms.</p>
 */
public class MarkdownDocCommentsExample {

    /// Formats a score label.
    ///
    /// Java 22 traditional Javadoc for the same idea would typically look like:
    /// ```java
    /// /**
    ///  * Formats a score label.
    ///  *
    ///  * @param name  player name
    ///  * @param score points earned
    ///  * @return a label such as {@code Alice: 10}
    ///  {@literal *}/
    /// ```
    ///
    /// @param name  player name
    /// @param score points earned
    /// @return a label such as `Alice: 10`
    static String formatScore(String name, int score) {
        return name + ": " + score;
    }

    public static void main(String[] args) {
        // Java 22 style (conceptual): traditional /** */ Javadoc with HTML tags.
        String java22Style = String.join("\n",
                "/**",
                " * Formats a score label.",
                " *",
                " * @param name  player name",
                " * @param score points earned",
                " * @return a label such as {@code Alice: 10}",
                " */");
        System.out.println("Java 22 traditional Javadoc comment:");
        System.out.println(java22Style);

        // Java 23 style (conceptual): /// Markdown documentation comment.
        String java23Style = String.join("\n",
                "/// Formats a score label.",
                "///",
                "/// @param name  player name",
                "/// @param score points earned",
                "/// @return a label such as `Alice: 10`");
        System.out.println();
        System.out.println("Java 23 Markdown documentation comment (JEP 467):");
        System.out.println(java23Style);

        // The code behavior is unchanged; only the documentation syntax differs.
        List<String> labels = List.of(formatScore("Alice", 10), formatScore("Bob", 8));
        System.out.println();
        System.out.println("Runtime behavior is the same: " + labels);
        System.out.println("Traditional /** */ comments still work in Java 23.");
    }
}
