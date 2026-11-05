package com.example.java8.base64;

import java.util.Base64;

public class Base64Example {
    public static void main(String[] args) {
        String original = "Hello Java8";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes());
        String decoded = new String(Base64.getDecoder().decode(encoded));

        System.out.println("Original: " + original);
        System.out.println("Encoded: " + encoded);
        System.out.println("Decoded: " + decoded);

        // Realtime usage: encoding credentials, logging, data URI
    }
}