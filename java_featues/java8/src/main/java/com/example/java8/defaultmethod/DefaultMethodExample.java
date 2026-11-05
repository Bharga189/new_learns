package com.example.java8.defaultmethod;

interface Base {
    default void greet() {
        System.out.println("Hello from default method");
    }
}

interface Derived extends Base {
    @Override
    default void greet() {
        System.out.println("Hello from derived");
    }
}

public class DefaultMethodExample {
    public static void main(String[] args) {
        Derived d = new Derived() {};
        d.greet(); // prints derived version
        // Realtime usage: adding new methods to interfaces without breaking existing implementations
    }
}