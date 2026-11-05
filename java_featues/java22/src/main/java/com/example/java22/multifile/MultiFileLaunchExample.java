package com.example.java22.multifile;

/**
 * Java 22: Launch Multi-File Source-Code Programs (JEP 458)
 *
 * Java 11-21: the source-file launcher (java Hello.java) ran a program made of
 * a single source file. A program split across several files had to be compiled
 * with javac first, or inlined into one file.
 *
 * Java 22: java Main.java also compiles and runs other source files that the
 * launched file references in the same directory, on demand.
 *
 * Try it from this directory:
 *   java MultiFileLaunchExample.java
 *
 * With Java 21 the same command cannot resolve GreetingHelper unless the
 * classes were compiled beforehand.
 */
public class MultiFileLaunchExample {

    public static void main(String[] args) {
        // References a second source file; the Java 22 launcher compiles it on demand.
        System.out.println("Java 22 multi-file launch: " + GreetingHelper.greeting("world"));
    }
}
