# -*- coding: utf-8 -*-
"""
Feature 2: Operators
Arithmetic, comparison, logical, identity, membership, bitwise, walrus.
"""

def demo() -> None:
    print("\n" + "=" * 70)
    print("2. OPERATORS")
    print("=" * 70)

    print("\n--- Arithmetic Operators ---")
    a, b = 10, 3
    print(f"{a} + {b} = {a + b}")
    print(f"{a} - {b} = {a - b}")
    print(f"{a} * {b} = {a * b}")
    print(f"{a} / {b} = {a / b}")
    print(f"{a} // {b} = {a // b}")
    print(f"{a} % {b} = {a % b}")
    print(f"{a} ** {b} = {a ** b}")

    print("\n--- Walrus Operator (:=) ---")
    if (n := len([1, 2, 3, 4, 5])) > 0:
        print(f"List has {n} elements")

    print("\n--- Assignment Expressions ---")
    values = [1, 2, 3, 4, 5]
    print(f"Sum (walrus): {sum(v := values)}")

    print("\n--- Comparison Operators ---")
    x, y = 5, 10
    print(f"{x} == {y}: {x == y}")
    print(f"{x} != {y}: {x != y}")
    print(f"{x} > {y}: {x > y}")
    print(f"{x} < {y}: {x < y}")
    print(f"{x} >= {y}: {x >= y}")
    print(f"{x} <= {y}: {x <= y}")

    print("\n--- Chained Comparisons ---")
    print(f"1 < 2 < 3: {1 < 2 < 3}")
    print(f"2 <= 2 < 3: {2 <= 2 < 3}")

    print("\n--- Logical Operators ---")
    p, q = True, False
    print(f"{p} and {q}: {p and q}")
    print(f"{p} or {q}: {p or q}")
    print(f"not {p}: {not p}")

    print("\n--- Identity Operators ---")
    list1 = [1, 2, 3]
    list2 = list1
    list3 = [1, 2, 3]
    print(f"list1 is list2: {list1 is list2}")
    print(f"list1 is list3: {list1 is list3}")
    print(f"list1 == list3: {list1 == list3}")

    print("\n--- Membership Operators ---")
    numbers = [1, 2, 3, 4, 5]
    print(f"3 in numbers: {3 in numbers}")
    print(f"10 not in numbers: {10 not in numbers}")

    print("\n--- Bitwise Operators ---")
    print(f"0b1010 & 0b1100 = {0b1010 & 0b1100}")
    print(f"0b1010 | 0b1100 = {0b1010 | 0b1100}")
    print(f"0b1010 ^ 0b1100 = {0b1010 ^ 0b1100}")
    print(f"~0b1010 = {~0b1010}")
