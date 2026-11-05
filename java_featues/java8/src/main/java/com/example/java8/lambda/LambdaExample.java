package com.example.java8.lambda;

@FunctionalInterface interface Converter {
    String convert(String s);
}

public class LambdaExample {
    public static void main(String[] args) {
        // Traditional anonymous class
        Converter uppercase = new Converter() {
            public String convert(String s) {
                return s.toUpperCase();
            }
        };

        // Lambda expression
        Converter lowercase = s -> s.toLowerCase();

        System.out.println(uppercase.convert("hello"));
        System.out.println(lowercase.convert("HELLO"));
        // Realtime usage: transforming user input, processing streams, event handlers
    }
}