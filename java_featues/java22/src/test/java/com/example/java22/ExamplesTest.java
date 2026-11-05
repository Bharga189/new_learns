package com.example.java22;

import com.example.java22.console.ConsoleIsTerminalExample;
import com.example.java22.foreignmemory.ForeignMemoryExample;
import com.example.java22.multifile.MultiFileLaunchExample;
import com.example.java22.unnamedvariables.UnnamedVariablesExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 22 example runs and writes output. */
class ExamplesTest {

    @Test
    void unnamedVariablesExampleRuns() {
        assertRuns("UnnamedVariablesExample", () -> UnnamedVariablesExample.main(new String[0]));
    }

    @Test
    void foreignMemoryExampleRuns() {
        assertRuns("ForeignMemoryExample", () -> ForeignMemoryExample.main(new String[0]));
    }

    @Test
    void consoleIsTerminalExampleRuns() {
        assertRuns("ConsoleIsTerminalExample", () -> ConsoleIsTerminalExample.main(new String[0]));
    }

    @Test
    void multiFileLaunchExampleRuns() {
        assertRuns("MultiFileLaunchExample", () -> MultiFileLaunchExample.main(new String[0]));
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
