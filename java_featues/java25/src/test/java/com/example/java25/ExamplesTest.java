package com.example.java25;

import com.example.java25.compactsource.CompactSourceExample;
import com.example.java25.flexibleconstructors.FlexibleConstructorsExample;
import com.example.java25.moduleimport.ModuleImportExample;
import com.example.java25.scopedvalues.ScopedValuesExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 25 example runs and writes output. */
class ExamplesTest {

    @Test
    void flexibleConstructorsExampleRuns() {
        assertRuns("FlexibleConstructorsExample", () -> FlexibleConstructorsExample.main(new String[0]));
    }

    @Test
    void scopedValuesExampleRuns() {
        assertRuns("ScopedValuesExample", () -> ScopedValuesExample.main(new String[0]));
    }

    @Test
    void moduleImportExampleRuns() {
        assertRuns("ModuleImportExample", () -> ModuleImportExample.main(new String[0]));
    }

    @Test
    void compactSourceExampleRuns() {
        assertRuns("CompactSourceExample", () -> CompactSourceExample.main(new String[0]));
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
