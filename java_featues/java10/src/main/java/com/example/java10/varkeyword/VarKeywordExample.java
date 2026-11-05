package com.example.java10.varkeyword;

/**
 * Java 10 var Keyword Example
 * 
 * Java 10 introduced the `var` keyword for local variable type inference,
 * allowing the compiler to determine the type from the initializing value.
 * 
 * Key points about var:
 * - var is not a keyword - it's a reserved type name
 * - var must be initialized in the declaration
 * - The type is determined at compile-time, not runtime
 * - var can be used for arrays, collections, streams, and most local variables
 * - var cannot be used for method parameters, return types, or fields
 * 
 * Java 8 equivalent:
 *   List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
 *   
 * Java 10 with var:
 *   var names = Arrays.asList("Alice", "Bob", "Charlie");
 * 
 * Benefits:
 * - Reduced verbosity, especially with complex generic types
 * - Improved readability in some cases
 * - Maintains type safety
 * 
 * Realtime usage: Modernizing code, reducing boilerplate,
 * working with complex generic types in streams and lambdas
 */
public class VarKeywordExample {

    public static void main(String[] args) {
        // Java 8 verbose style
        java.util.List<String> java8Names = java.util.Arrays.asList("Alice", "Bob", "Charlie");
        System.out.println("Java 8: " + java8Names.get(0));
        
        // Java 10 var keyword - type inferred from initialization
        var java9Names = java.util.Arrays.asList("Alice", "Bob", "Charlie");
        System.out.println("Java 10 var: " + java9Names.get(0));
        
        // var with arrays
        var numbers = new int[]{1, 2, 3, 4, 5};
        System.out.println("var array: " + numbers[0]);
        
        // var with streams
        var uppercaseNames = java9Names.stream()
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("var stream: " + uppercaseNames);
        
        // Realtime usage: Reducing verbosity with complex types,
        // modernizing existing codebases, stream operations
    }
}