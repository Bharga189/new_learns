package com.example.java11;

import com.example.java11.collection.CollectionToArrayExample;
import com.example.java11.localvarlambda.LambdaVarExample;
import com.example.java11.optional.OptionalIsEmptyExample;
import com.example.java11.predicate.PredicateNotExample;
import com.example.java11.stringmethods.StringMethodsExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests: every Java 11 example runs and writes output. */
class ExamplesTest {

    @Test
    void stringMethodsExampleRuns() {
        assertRuns("StringMethodsExample", () -> StringMethodsExample.main(new String[0]));
    }

    @Test
    void lambdaVarExampleRuns() {
        assertRuns("LambdaVarExample", () -> LambdaVarExample.main(new String[0]));
    }

    @Test
    void optionalIsEmptyExampleRuns() {
        assertRuns("OptionalIsEmptyExample", () -> OptionalIsEmptyExample.main(new String[0]));
    }

    @Test
    void predicateNotExampleRuns() {
        assertRuns("PredicateNotExample", () -> PredicateNotExample.main(new String[0]));
    }

    @Test
    void collectionToArrayExampleRuns() {
        assertRuns("CollectionToArrayExample", () -> CollectionToArrayExample.main(new String[0]));
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
