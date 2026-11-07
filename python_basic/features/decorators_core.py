# -*- coding: utf-8 -*-
"""
Feature 17a: Decorators - Core Patterns
First-class functions, closures, higher-order functions, timing, logging.
"""

from functools import wraps
import time


def create_logging_decorator() -> callable:
    def decorator(func: callable) -> callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            import logging
            logging.info(f"Calling {func.__name__} with args={args}, kwargs={kwargs}")
            result = func(*args, **kwargs)
            logging.info(f"{func.__name__} returned {result}")
            return result
        return wrapper
    return decorator


def timing_decorator(func: callable) -> callable:
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        end = time.perf_counter()
        print(f"  {func.__name__} took {end - start:.4f} seconds")
        return result
    return wrapper


def demo() -> None:
    import logging
    print("\n" + "---" * 20)
    print("17a. DECORATORS - CORE PATTERNS")
    print("---" * 20)

    print("\n1.1 Basic Decorator:")
    @timing_decorator
    def slow_function(seconds: float) -> str:
        import time
        time.sleep(seconds)
        return f"Slept for {seconds} seconds"

    print("  Calling slow_function(0.1):")
    result = slow_function(0.1)
    print(f"  Result: {result}")

    print("\n1.2 Decorator with Arguments:")
    logging.getLogger().setLevel(logging.WARNING)

    @create_logging_decorator()
    def add(a: int, b: int) -> int:
        return a + b

    print("  Calling add(3, 4):")
    result = add(3, 4)
    print(f"  Result: {result}")

    print("\n1.3 Closures:")
    def make_multiplier(factor: int):
        def multiplier(x: int) -> int:
            return x * factor
        return multiplier

    double = make_multiplier(2)
    triple = make_multiplier(3)
    print(f"  double(5): {double(5)}")
    print(f"  triple(5): {triple(5)}")

    print("\n1.4 Higher-Order Functions:")
    numbers = [1, 2, 3, 4, 5]
    squared = list(map(lambda x: x ** 2, numbers))
    evens = list(filter(lambda x: x % 2 == 0, numbers))
    print(f"  map squares: {squared}")
    print(f"  filter evens: {evens}")
