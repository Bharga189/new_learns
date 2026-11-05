package com.example.java8.functional;

@FunctionalInterface
interface Calculator {
    int apply(int a, int b);
}

public class FunctionalInterfaceExample {
    public static void main(String[] args) {
        Calculator sum = (a, b) -> a + b;
        System.out.println("Sum: " + sum.apply(5, 3));

        // Realtime usage: passing behavior as argument, callbacks
    }
}