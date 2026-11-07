# -*- coding: utf-8 -*-
"""
Feature 9: Sets
Creation, operations, methods, removing duplicates, comprehensions.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("9. SETS")
    print("---" * 20)

    empty_set: set = set()
    set_literal: set = {1, 2, 3, 4, 5}
    set_from_list: set = set([1, 2, 2, 3, 3, 4])

    print(f"Empty set: {empty_set}")
    print(f"Set literal: {set_literal}")
    print(f"Set from list: {set_from_list}")

    print("\n--- Set Operations ---")
    set1: set = {1, 2, 3, 4, 5}
    set2: set = {4, 5, 6, 7, 8}

    print(f"set1: {set1}")
    print(f"set2: {set2}")
    print(f"Union (set1 | set2): {set1 | set2}")
    print(f"Intersection (set1 & set2): {set1 & set2}")
    print(f"Difference (set1 - set2): {set1 - set2}")
    print(f"Symmetric diff (set1 ^ set2): {set1 ^ set2}")

    print("\n--- Set Methods ---")
    s: set = {1, 2, 3}
    print(f"Original: {s}")

    s.add(4)
    print(f"After add(4): {s}")

    s.update([5, 6, 7])
    print(f"After update([5,6,7]): {s}")

    s.discard(3)
    print(f"After discard(3): {s}")

    s.discard(100)
    print(f"After discard(100): {s}")

    print("\n--- Set Comprehensions ---")
    numbers: list = [1, 2, 2, 3, 3, 3, 4, 4, 4, 4]
    unique_squares: set = {x ** 2 for x in numbers}
    print(f"Unique squares: {unique_squares}")

    print("\n--- Removing Duplicates ---")
    duplicates: list = [1, 2, 2, 3, 3, 3, 4, 5, 5]
    unique: list = list(set(duplicates))
    print(f"Original: {duplicates}")
    print(f"Unique: {unique}")
