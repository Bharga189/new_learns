package com.example.java11.stringmethods;

/**
 * Java 11 String Methods Example
 * 
 * Java 11 added several new methods to the String class for
 * common operations that previously required third-party libraries
 * or verbose code.
 * 
 * Java 11 New String Methods:
 * 
 * 1. isBlank() - Checks if string is empty or contains only whitespace
 *    - Replaces: string.trim().isEmpty()
 * 
 * 2. lines() - Returns a Stream of lines divided by line terminators
 *    - Replaces: Pattern.compile("\\R").splitAsStream(string)
 * 
 * 3. repeat(int) - Repeats the string n times
 *    - Replaces: IntStream.range(0, n).mapToObj(i -> string).reduce("", String::concat)
 * 
 * 4. strip() - Removes leading and trailing whitespace (unlike trim(), Unicode-aware)
 *    - Replaces: string.trim() for whitespace stripping
 * 
 * 5. stripIndent() - Removes common indentation from triple-quoted strings
 *    - Useful for text blocks (Java 13+ but discussed in Java 11 context)
 * 
 * Example usage:
 * "  hello  ".isBlank()         // false
 * "  hello  ".isBlank()         // true  
 * "hello\nworld\n".lines().count() // 2
 * "ha".repeat(3)                // "hahaha"
 * "  hello  ".strip()           // "hello"
 * 
 * Realtime usage: Input validation, parsing multi-line strings,
 * reducing boilerplate for common string operations, text processing
 */
public class StringMethodsExample {

    public static void main(String[] args) {
        // Java 11: isBlank() - checks if string is empty or only whitespace
        String spaced = "  hello  ";
        System.out.println("isBlank: " + spaced.isBlank()); // false
        
        String onlySpace = "   ";
        System.out.println("only space isBlank: " + onlySpace.isBlank()); // true
        
        String empty = "";
        System.out.println("empty isBlank: " + empty.isBlank()); // true
        
        // Java 11: lines() - returns stream of lines
        String multiLine = "line1\nline2\nline3";
        System.out.println("Lines count: " + multiLine.lines().count()); // 3
        
        // Java 11: repeat() - repeat string n times
        String hello = "hello";
        System.out.println("repeated: " + hello.repeat(3)); // "hellohellohello"
        
        // Java 11: strip() - Unicode-aware trim
        String withTabs = "\thello world\t";
        System.out.println("stripped: '" + withTabs.strip() + "'"); // "hello world"
        
        // Realtime usage: Input validation, parsing multi-line strings,
        // reducing boilerplate for common string operations, text processing
    }
}