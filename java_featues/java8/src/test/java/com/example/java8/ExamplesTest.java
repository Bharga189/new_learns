package com.example.java8;

import com.example.java8.base64.Base64Example;
import com.example.java8.defaultmethod.DefaultMethodExample;
import com.example.java8.functional.FunctionalInterfaceExample;
import com.example.java8.lambda.LambdaExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 8 example runs and writes output. */
class ExamplesTest {

    @Test
    void base64ExampleRuns() {
        assertRuns("Base64Example", () -> Base64Example.main(new String[0]));
    }

    @Test
    void defaultMethodExampleRuns() {
        assertRuns("DefaultMethodExample", () -> DefaultMethodExample.main(new String[0]));
    }

    @Test
    void functionalInterfaceExampleRuns() {
        assertRuns("FunctionalInterfaceExample", () -> FunctionalInterfaceExample.main(new String[0]));
    }

    @Test
    void lambdaExampleRuns() {
        assertRuns("LambdaExample", () -> LambdaExample.main(new String[0]));
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
