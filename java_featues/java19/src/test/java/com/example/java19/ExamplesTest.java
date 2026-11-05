package com.example.java19;

import com.example.java19.comparison.collectioncomparison.Java19CollectionExample;
import com.example.java19.comparison.optionalcomparison.Java19OptionalExample;
import com.example.java19.comparison.streamcomparison.Java19StreamExample;
import com.example.java19.patternmatching.InstanceofPatternExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 19 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java19CollectionExample", () -> Java19CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java19OptionalExample", () -> Java19OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java19StreamExample", () -> Java19StreamExample.main(new String[0]));
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
