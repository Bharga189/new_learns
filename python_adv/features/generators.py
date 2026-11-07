# -*- coding: utf-8 -*-
"""
Feature 3: Generators and Iterators
Iterator protocol, custom iterators, generators, generator expressions, yield.
"""

import itertools


def count_up_to(n):
    """Generator that counts up to n"""
    count = 1
    while count <= n:
        yield count
        count += 1


def fibonacci(n):
    """Generator that yields Fibonacci sequence"""
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b


def even_numbers(limit):
    """Generator that yields even numbers up to limit"""
    num = 0
    while num <= limit:
        yield num
        num += 2


def generator_demo() -> None:
    print("\n" + "=" * 70)
    print("3. GENERATORS AND ITERATORS")
    print("=" * 70)

    # Basic generator
    print("\n3.1 Basic Generator:")
    print("  count_up_to(5):")
    for num in count_up_to(5):
        print(f"    {num}", end=" ")
    print()

    # Fibonacci generator
    print("\n3.2 Fibonacci Generator:")
    print("  First 10 Fibonacci numbers:")
    for num in fibonacci(10):
        print(f"    {num}", end=" ")
    print()

    # Even numbers generator
    print("\n3.3 Even Numbers Generator:")
    print("  Even numbers up to 20:")
    for num in even_numbers(20):
        print(f"    {num}", end=" ")
    print()

    # Generator expression
    print("\n3.4 Generator Expression:")
    # Generator expression - memory efficient
    squares_gen = (x ** 2 for x in range(10))
    print(f"  squares_gen: {squares_gen}")
    print(f"  List of squares: {[x for x in squares_gen]}")

    # Memory comparison
    print("\n3.5 Memory Comparison:")
    print("  List comprehension: [x**2 for x in range(1000000)]")
    print("  Creates entire list in memory")
    print("  Generator: (x**2 for x in range(1000000))")
    print("  Yields one item at a time")

    # Generator methods
    print("\n3.6 Generator Methods:")
    def countdown(n):
        while n > 0:
            received = yield n
            print(f"    Received: {received}")
            n -= 1

    gen = countdown(3)
    print("  Next(gen):", next(gen))
    print("  Next gen after send:", end=" ")
    print(next(gen))
    print("  send(gen, 'Hello'):", end=" ")
    gen.send("Hello")
    try:
        print("  Next gen:", next(gen))
    except StopIteration:
        print("  Generator exhausted")
    gen.close()
    print("  Generator closed")


def countdown(n):
    """Custom iterator demo using generator"""
    while n > 0:
        yield n
        n -= 1


if __name__ == "__main__":
    generator_demo()
