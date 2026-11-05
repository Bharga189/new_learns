package com.example.java15;

import com.example.java15.comparison.collectioncomparison.Java15CollectionExample;
import com.example.java15.comparison.optionalcomparison.Java15OptionalExample;
import com.example.java15.comparison.streamcomparison.Java15StreamExample;
import com.example.java15.textblocks.TextBlocksExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 15 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java15CollectionExample", () -> Java15CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java15OptionalExample", () -> Java15OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java15StreamExample", () -> Java15StreamExample.main(new String[0]));
    }

    @Test
    void textBlocksExampleRuns() {
        assertRuns("TextBlocksExample", () -> TextBlocksExample.main(new String[0]));
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
