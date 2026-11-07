# -*- coding: utf-8 -*-
"""
Feature 1: Advanced Functions and Closures
First-class functions, closures, higher-order functions, partial functions.
"""

import functools


def advanced_functions_demo() -> None:
    print("\n" + "=" * 70)
    print("1. ADVANCED FUNCTIONS AND CLOSURES")
    print("=" * 70)

    # First-class functions
    print("\n1.1 First-Class Functions:")
    def greet(name):
        return f"Hello, {name}!"

    def farewell(name):
        return f"Goodbye, {name}!"

    def process_name(func, name):
        """Higher-order function - takes a function as argument"""
        return func(name)

    print(f"  process_name(greet, 'Alice'): {process_name(greet, 'Alice')}")
    print(f"  process_name(farewell, 'Bob'): {process_name(farewell, 'Bob')}")

    # Closures
    print("\n1.2 Closures:")
    def make_multiplier(factor):
        """Returns a function that multiplies by factor"""
        def multiplier(x):
            return x * factor
        return multiplier

    double = make_multiplier(2)
    triple = make_multiplier(3)
    print(f"  double(5): {double(5)}")
    print(f"  triple(5): {triple(5)}")

    # Inner function remembers the factor
    print(f"  Closure check: double.__closure__ is not None: {double.__closure__ is not None}")

    # Partial functions
    print("\n1.3 Partial Functions:")
    def power(base, exponent):
        return base ** exponent

    # Create a partial function with fixed exponent
    square = functools.partial(power, exponent=2)
    cube = functools.partial(power, exponent=3)

    print(f"  square(5): {square(5)}")
    print(f"  cube(5): {cube(5)}")

    # Partial with position arguments
    def greet_custom(greeting, name, punctuation="!"):
        return f"{greeting}, {name}{punctuation}"

    friendly_greet = functools.partial(greet_custom, name="World")
    print(f"  friendly_greet('Hi'): {friendly_greet('Hi')}")
    print(f"  friendly_greet('Hi', punctuation='?'): {friendly_greet('Hi', punctuation='?')}")


if __name__ == "__main__":
    advanced_functions_demo()
