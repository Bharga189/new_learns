package com.example.java10;

import com.example.java10.comparison.optionalcomparison.Java10OptionalExample;
import com.example.java10.varkeyword.VarKeywordExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 10 example runs and writes output. */
class ExamplesTest {

    @Test
    void optionalExampleRuns() {
        assertRuns("Java10OptionalExample", () -> Java10OptionalExample.main(new String[0]));
    }

    @Test
    void varKeywordExampleRuns() {
        assertRuns("VarKeywordExample", () -> VarKeywordExample.main(new String[0]));
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
