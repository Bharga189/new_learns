package com.example.java14;

import com.example.java14.comparison.collectioncomparison.Java14CollectionExample;
import com.example.java14.comparison.optionalcomparison.Java14OptionalExample;
import com.example.java14.comparison.streamcomparison.Java14StreamExample;
import com.example.java14.switchdemo.SwitchExpressionExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 14 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java14CollectionExample", () -> Java14CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java14OptionalExample", () -> Java14OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java14StreamExample", () -> Java14StreamExample.main(new String[0]));
    }

    @Test
    void switchExpressionExampleRuns() {
        assertRuns("SwitchExpressionExample", () -> SwitchExpressionExample.main(new String[0]));
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
