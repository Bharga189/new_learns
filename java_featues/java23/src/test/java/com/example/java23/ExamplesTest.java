package com.example.java23;

import com.example.java23.markdowndoc.MarkdownDocCommentsExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 23 example runs and writes output. */
class ExamplesTest {

    @Test
    void markdownDocCommentsExampleRuns() {
        assertRuns("MarkdownDocCommentsExample", () -> MarkdownDocCommentsExample.main(new String[0]));
    }

    private static void assertRuns(String example, Executable executable) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output, true));
            assertDoesNotThrow(executable, example + " threw an exception");
        } finally {
            System.setOut(original);
        }
        assertTrue(output.toString().trim().length() > 0, example + " produced no output");
    }
}
