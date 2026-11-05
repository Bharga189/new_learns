package com.example.java16;

import com.example.java16.comparison.collectioncomparison.Java16CollectionExample;
import com.example.java16.comparison.optionalcomparison.Java16OptionalExample;
import com.example.java16.comparison.streamcomparison.Java16StreamExample;
import com.example.java16.patternmatching.InstanceofPatternExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 16 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java16CollectionExample", () -> Java16CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java16OptionalExample", () -> Java16OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java16StreamExample", () -> Java16StreamExample.main(new String[0]));
    }

    @Test
    void instanceofPatternExampleRuns() {
        assertRuns("InstanceofPatternExample", () -> InstanceofPatternExample.main(new String[0]));
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
