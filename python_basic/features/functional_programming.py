# -*- coding: utf-8 -*-
"""
Feature 16: Functional Programming
Pure functions, immutability, map/filter/reduce, function composition, currying, partial.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("16. FUNCTIONAL PROGRAMMING")
    print("---" * 20)

    print("\n--- Pure Functions ---")
    def add(a: int, b: int) -> int:
        return a + b

    print(f"add(3, 4) = {add(3, 4)}")
    print(f"add(3, 4) = {add(3, 4)}")

    print("\n--- Immutability ---")
    original: tuple = (1, 2, 3)
    new: tuple = original + (4,)
    print(f"Original tuple: {original}")
    print(f"New tuple: {new}")
    print("Original unchanged:", original)

    text: str = "hello"
    modified: str = text.replace("h", "H")
    print(f"Original: {text}")
    print(f"Modified: {modified}")

    print("\n--- map, filter, reduce ---")
    numbers: list = [1, 2, 3, 4, 5]

    squared: list = list(map(lambda x: x ** 2, numbers))
    print(f"map squares: {squared}")

    evens: list = list(filter(lambda x: x % 2 == 0, numbers))
    print(f"filter evens: {evens}")

    from functools import reduce
    product: int = reduce(lambda x, y: x * y, numbers)
    print(f"reduce product: {product}")

    print("\n--- Function Composition ---")
    def double(x: int) -> int:
        return x * 2

    def increment(x: int) -> int:
        return x + 1

    def compose(f: callable, g: callable) -> callable:
        def composed(x: any) -> any:
            return f(g(x))
        return composed

    double_then_increment = compose(double, increment)
    print(f"double(increment(3)) = {double_then_increment(3)}")

    print("\n--- Currying and Partial Application ---")
    def curry_add(a: int) -> callable:
        def add_b(b: int) -> int:
            return a + b
        return add_b

    add5: callable = curry_add(5)
    print(f"curry_add(5)(3) = {add5(3)}")

    def power(base: int, exponent: int) -> int:
        return base ** exponent

    from functools import partial
    square: callable = partial(power, exponent=2)
    print(f"partial power(2): {square(5)}")
