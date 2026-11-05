package com.example.java9;

import com.example.java9.moduleinfo.ModuleInfo;
import com.example.java9.optionalenhancements.OptionalEnhancements;
import com.example.java9.privateinterface.PrivateInterfaceMethod;
import com.example.java9.processapi.ProcessApiDemo;
import com.example.java9.streamenhancements.StreamEnhancements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 9 example runs and writes output. */
class ExamplesTest {

    @Test
    void moduleInfoExampleRuns() {
        assertRuns("ModuleInfo", () -> ModuleInfo.main(new String[0]));
    }

    @Test
    void optionalEnhancementsExampleRuns() {
        assertRuns("OptionalEnhancements", () -> OptionalEnhancements.main(new String[0]));
    }

    @Test
    void privateInterfaceMethodExampleRuns() {
        assertRuns("PrivateInterfaceMethod", () -> PrivateInterfaceMethod.main(new String[0]));
    }

    @Test
    void processApiDemoExampleRuns() {
        assertRuns("ProcessApiDemo", () -> ProcessApiDemo.main(new String[0]));
    }

    @Test
    void streamEnhancementsExampleRuns() {
        assertRuns("StreamEnhancements", () -> StreamEnhancements.main(new String[0]));
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
