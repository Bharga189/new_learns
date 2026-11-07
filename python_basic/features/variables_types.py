# -*- coding: utf-8 -*-
"""
Feature 1: Variables and Data Types
Modern type hints, pattern matching (Python 3.10+).
"""

def demo() -> None:
    print("=" * 70)
    print("1. VARIABLES AND DATA TYPES")
    print("=" * 70)

    integer_var: int = 42
    float_var: float = 3.14159
    string_var: str = "Hello, Python!"
    boolean_var: bool = True
    none_var: None = None

    print(f"Integer: {integer_var} (type: {type(integer_var).__name__})")
    print(f"Float: {float_var} (type: {type(float_var).__name__})")
    print(f"String: {string_var} (type: {type(string_var).__name__})")
    print(f"Boolean: {boolean_var} (type: {type(boolean_var).__name__})")
    print(f"None: {none_var} (type: {type(none_var).__name__})")

    print("\n--- Type Conversion ---")
    print(f"int(3.9) = {int(3.9)}")
    print(f"float(5) = {float(5)}")
    print(f"str(100) = {str(100)}")

    print(f"bool(0) = {bool(0)}")
    print(f"bool(1) = {bool(1)}")
    print(f"bool('') = {bool('')}")
    print(f"bool('hello') = {bool('hello')}")

    print("\n--- Pattern Matching (Python 3.10+) ---")
    match integer_var:
        case 0:
            print("Zero")
        case 42:
            print(f"Answer to everything: {integer_var}")
        case _ if integer_var > 0:
            print(f"Positive number: {integer_var}")
        case _:
            print("Other")
