package com.example.java22.multifile;

/**
 * A separate source file referenced by {@link MultiFileLaunchExample}.
 *
 * Under JEP 458 the Java 22 source launcher compiles this file on demand when
 * the launched program uses it.
 */
final class GreetingHelper {

    private GreetingHelper() {
    }

    static String greeting(String name) {
        return "Hello, " + name + "!";
    }
}
