package com.example.java20;

import com.example.java20.comparison.collectioncomparison.Java20CollectionExample;
import com.example.java20.comparison.optionalcomparison.Java20OptionalExample;
import com.example.java20.comparison.streamcomparison.Java20StreamExample;
import com.example.java20.patternmatching.InstanceofPatternExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 20 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java20CollectionExample", () -> Java20CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java20OptionalExample", () -> Java20OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java20StreamExample", () -> Java20StreamExample.main(new String[0]));
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
