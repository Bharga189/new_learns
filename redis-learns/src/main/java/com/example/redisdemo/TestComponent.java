package com.example.redisdemo;

import org.springframework.stereotype.Component;

@Component
public class TestComponent {
    public String getMessage() { return "Hello from test component"; }
}