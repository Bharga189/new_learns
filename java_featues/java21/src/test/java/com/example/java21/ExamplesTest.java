package com.example.java21;

import com.example.java21.recordpatterns.RecordPatternsExample;
import com.example.java21.sequencedcollections.SequencedCollectionsExample;
import com.example.java21.switchpatterns.PatternMatchingSwitchExample;
import com.example.java21.virtualthreads.VirtualThreadsExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 21 example runs and writes output. */
class ExamplesTest {

    @Test
    void sequencedCollectionsExampleRuns() {
        assertRuns("SequencedCollectionsExample", () -> SequencedCollectionsExample.main(new String[0]));
    }

    @Test
    void recordPatternsExampleRuns() {
        assertRuns("RecordPatternsExample", () -> RecordPatternsExample.main(new String[0]));
    }

    @Test
    void patternMatchingSwitchExampleRuns() {
        assertRuns("PatternMatchingSwitchExample", () -> PatternMatchingSwitchExample.main(new String[0]));
    }

    @Test
    void virtualThreadsExampleRuns() {
        assertRuns("VirtualThreadsExample", () -> VirtualThreadsExample.main(new String[0]));
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
