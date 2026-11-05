package com.example.java22.console;

import java.io.Console;

/**
 * Java 22: Console.isTerminal()
 *
 * Java 21: System.console() returned null when input or output was redirected,
 * but for an attached console there was no portable way to tell whether it was
 * connected to a real terminal. Programs guessed with environment variables or
 * native libraries.
 *
 * Java 22: Console.isTerminal() reports whether the console is attached to a
 * terminal, so interactive prompts can be enabled only when appropriate.
 */
public class ConsoleIsTerminalExample {

    public static void main(String[] args) {
        Console console = System.console();

        // Java 21 style: "is a console attached at all?"
        System.out.println("Java 21 System.console() present: " + (console != null));

        // Java 22: "is that console an actual terminal?"
        if (console == null) {
            System.out.println("Java 22 isTerminal(): no console attached");
        } else {
            System.out.println("Java 22 isTerminal(): " + console.isTerminal());
        }
    }
}
