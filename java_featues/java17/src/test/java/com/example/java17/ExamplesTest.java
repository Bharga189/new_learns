package com.example.java17;

import com.example.java17.comparison.collectioncomparison.Java17CollectionExample;
import com.example.java17.comparison.optionalcomparison.Java17OptionalExample;
import com.example.java17.comparison.streamcomparison.Java17StreamExample;
import com.example.java17.patternmatching.InstanceofPatternExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 17 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java17CollectionExample", () -> Java17CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java17OptionalExample", () -> Java17OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java17StreamExample", () -> Java17StreamExample.main(new String[0]));
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
