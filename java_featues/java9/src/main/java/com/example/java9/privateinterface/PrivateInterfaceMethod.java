package com.example.java9.privateinterface;

import java.util.Arrays;
import java.util.List;

/**
 * Java 9 Private Methods in Interfaces Example
 * 
 * Java 9 introduced the ability to declare private methods in interfaces,
 * which can be used by default methods within the same interface.
 * 
 * Before Java 9:
 * - Interface methods were implicitly public and abstract
 * - Default methods allowed adding new methods to interfaces without breaking
 *   existing implementations
 * - But there was no way to share private helper logic between default methods
 * 
 * Java 9 Features:
 * - private interface methods: Declared with 'private' keyword
 * - Can be used inside default methods of the same interface
 * - Cannot be overridden by implementing classes
 * - Can be overloaded (private static and private instance methods)
 * 
 * Benefits:
 * - Better code organization in interfaces
 * - Helper methods can be hidden from outside world
 * - Default methods can reuse logic without code duplication
 * - Backward compatible - existing code continues to work
 * 
 * Interface structure example:
 * interface MyInterface {
 *     default void process() {
 *         helperMethod(); // Can call private method
 *         System.out.println("Processing...");
 *     }
 *     
 *     private void helperMethod() {
 *         // Implementation hidden from outside
 *     }
 * }
 * 
 * Realtime usage: Clean interface design, utility methods in functional interfaces,
 * organizing logic without exposing it to implementing classes
 */
public class PrivateInterfaceMethod {

    public static void main(String[] args) {
        // Java 9: Private method in interface
        DefaultInterface obj = new DefaultInterface() {};
        obj.perform(); // Output: Helper method / Performing task...
        obj.display();
        
        // Realtime usage: Clean interface design, utility methods 
        // in functional interfaces, organizing logic without exposing
    }
}

/**
 * Interface demonstrating Java 9 private interface methods
 */
interface DefaultInterface {
    // Default method
    default void display() {
        System.out.println("Default method display");
    }
    
    // Java 9: Private method - can only be used within this interface
    private void helper() {
        System.out.println("Helper method called");
    }
    
    // Default method that uses private method
    default void perform() {
        helper(); // OK: private method accessible within interface
        System.out.println("Performing task...");
    }
}