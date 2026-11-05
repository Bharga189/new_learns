package com.example.java18;

import com.example.java18.comparison.collectioncomparison.Java18CollectionExample;
import com.example.java18.comparison.optionalcomparison.Java18OptionalExample;
import com.example.java18.comparison.streamcomparison.Java18StreamExample;
import com.example.java18.patternmatching.InstanceofPatternExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 18 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java18CollectionExample", () -> Java18CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java18OptionalExample", () -> Java18OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java18StreamExample", () -> Java18StreamExample.main(new String[0]));
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
