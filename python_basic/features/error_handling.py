# -*- coding: utf-8 -*-
"""
Feature 11: Error Handling
try/except, multiple exceptions, else/finally, raising, custom exceptions, chaining.
"""

class CustomError(Exception):
    """Custom exception example."""

    def __init__(self, message: str, code: int = None):
        super().__init__(message)
        self.code = code


def demo() -> None:
    print("\n" + "---" * 20)
    print("11. ERROR HANDLING")
    print("---" * 20)

    print("\nBasic exception handling:")
    try:
        result = 10 / 0
    except ZeroDivisionError as e:
        print(f"  Caught: {type(e).__name__}: {e}")

    print("\nMultiple exception types:")
    test_values: list = [10, 0, "hello", 5]
    for val in test_values:
        try:
            result = 100 / val
            print(f"  100 / {val} = {result}")
        except ZeroDivisionError:
            print(f"  Can't divide by zero: {val}")
        except TypeError:
            print(f"  Type error with: {val}")

    print("\ntry/except/else/finally:")
    try:
        result = 10 / 2
    except ZeroDivisionError:
        print("  Division by zero!")
    else:
        print(f"  No exception, result: {result}")
    finally:
        print("  This always runs")

    print("\nRaising exceptions:")
    try:
        age: int = -5
        if age < 0:
            raise ValueError(f"Age can't be negative: {age}")
    except ValueError as e:
        print(f"  Caught: {e}")

    print("\nCustom exceptions:")
    try:
        raise CustomError("Something went wrong", code=500)
    except CustomError as e:
        print(f"  Custom error: {e}, code: {e.code}")

    print("\nUsing contextlib.suppress:")
    from contextlib import suppress
    with suppress(FileNotFoundError):
        open("nonexistent_file.txt").close()
    print("  Handled missing file silently")

    print("\nException chaining:")
    try:
        try:
            raise ValueError("Original error")
        except ValueError as e:
            raise RuntimeError("Wrapped error") from e
    except RuntimeError as e:
        print(f"  Caught: {e}")
        print(f"  __cause__: {e.__cause__}")
