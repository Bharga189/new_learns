package com.example.java9.moduleinfo;

/**
 * Java 9 Module System Example
 * 
 * Java 9 introduced the Java Platform Module System (JPMS), also known as Project Jigsaw.
 * Modules provide a way to organize code into named, reusable units with explicit dependencies.
 * 
 * Key module concepts:
 * - module-info.java: Describes the module and its dependencies
 * - encapsulation: Classes inside a module are hidden from other modules by default
 * - explicit dependencies: Modules declare what they require via 'requires'
 * - strong encapsulation: Breaks the reflection-based access that was previously possible
 * 
 * Module declaration example:
 * module com.example.myapp {
 *     requires java.base;
 *     requires java.sql;
 *     exports com.example.myapp.service;
 *     exports com.example.myapp.controller to com.example.db;
 * }
 * 
 * Benefits:
 * - Better maintainability of large codebases
 * - Improved performance (only loaded needed modules)
 * - Stronger encapsulation and security
 * - Easier dependency management
 * 
 * Realtime usage: Modularizing enterprise applications,
 * creating reusable libraries, improving security surface area
 */
public class ModuleInfo {

    public static void main(String[] args) {
        // Module system is configured via module-info.java, not programmatically
        // This class documents the module system concepts
        
        System.out.println("Java 9 Module System (JPMS)");
        System.out.println("Configure via module-info.java file");
        System.out.println("Benefits: Encapsulation, explicit dependencies, modular deployment");
        
        // Realtime usage: Large-scale application modularization,
        // creating internal APIs, reducing classpath complexity
    }
}