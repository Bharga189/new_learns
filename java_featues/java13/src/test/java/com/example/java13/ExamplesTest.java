package com.example.java13;

import com.example.java13.comparison.collectioncomparison.Java13CollectionExample;
import com.example.java13.comparison.optionalcomparison.Java13OptionalExample;
import com.example.java13.comparison.streamcomparison.Java13StreamExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 13 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java13CollectionExample", () -> Java13CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java13OptionalExample", () -> Java13OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java13StreamExample", () -> Java13StreamExample.main(new String[0]));
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
