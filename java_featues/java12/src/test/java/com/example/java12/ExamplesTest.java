package com.example.java12;

import com.example.java12.compactnumber.CompactNumberFormatExample;
import com.example.java12.comparison.collectioncomparison.Java12CollectionExample;
import com.example.java12.comparison.optionalcomparison.Java12OptionalExample;
import com.example.java12.comparison.streamcomparison.Java12StreamExample;
import com.example.java12.filesmismatch.FilesMismatchExample;
import com.example.java12.stringtransform.StringTransformExample;
import com.example.java12.teeing.TeeingCollectorExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 12 example runs and writes output. */
class ExamplesTest {

    @Test
    void collectionExampleRuns() {
        assertRuns("Java12CollectionExample", () -> Java12CollectionExample.main(new String[0]));
    }

    @Test
    void optionalExampleRuns() {
        assertRuns("Java12OptionalExample", () -> Java12OptionalExample.main(new String[0]));
    }

    @Test
    void streamExampleRuns() {
        assertRuns("Java12StreamExample", () -> Java12StreamExample.main(new String[0]));
    }

    @Test
    void teeingCollectorExampleRuns() {
        assertRuns("TeeingCollectorExample", () -> TeeingCollectorExample.main(new String[0]));
    }

    @Test
    void stringTransformExampleRuns() {
        assertRuns("StringTransformExample", () -> StringTransformExample.main(new String[0]));
    }

    @Test
    void filesMismatchExampleRuns() {
        assertRuns("FilesMismatchExample", () -> FilesMismatchExample.main(new String[0]));
    }

    @Test
    void compactNumberFormatExampleRuns() {
        assertRuns("CompactNumberFormatExample", () -> CompactNumberFormatExample.main(new String[0]));
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
