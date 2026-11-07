# -*- coding: utf-8 -*-
"""
Feature 13: List Comprehensions (Detailed)
Basic, with conditions, if-else, nested, flattening, dict/set comps.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("13. LIST COMPREHENSIONS")
    print("---" * 20)

    print("\n1. Basic comprehension:")
    numbers: list = list(range(10))
    squares: list = [x ** 2 for x in numbers]
    print(f"   Squares: {squares}")

    print("\n2. With condition:")
    evens: list = [x for x in numbers if x % 2 == 0]
    odds: list = [x for x in numbers if x % 2 != 0]
    print(f"   Evens: {evens}")
    print(f"   Odds: {odds}")

    print("\n3. With if-else (conditional expression):")
    labels: list = ["even" if x % 2 == 0 else "odd" for x in numbers]
    print(f"   Labels: {labels}")

    print("\n4. Nested comprehension:")
    matrix: list = [[i * j for j in range(5)] for i in range(5)]
    for row in matrix:
        print(f"   {row}")

    print("\n5. Flattening:")
    nested: list = [[1, 2, 3], [4, 5], [6, 7, 8, 9]]
    flat: list = [item for sublist in nested for item in sublist]
    print(f"   Flattened: {flat}")

    print("\n6. String operations:")
    words: list = ["hello", "world", "python", "programming"]
    upper_words: list = [word.upper() for word in words]
    word_lengths: list = [len(word) for word in words]
    print(f"   Upper: {upper_words}")
    print(f"   Lengths: {word_lengths}")

    print("\n7. Dict and Set comprehensions:")
    dict_comp: dict = {x: x ** 3 for x in range(5)}
    set_comp: set = {x % 3 for x in range(10)}
    print(f"   Dict: {dict_comp}")
    print(f"   Set: {set_comp}")

    print("\n8. Multiple conditions:")
    numbers = list(range(20))
    result: list = [x for x in numbers if x > 5 if x < 15 if x % 2 == 0]
    print(f"   5 < x < 15 and even: {result}")
