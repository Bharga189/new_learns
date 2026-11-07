# -*- coding: utf-8 -*-
"""
Feature 2: Decorators
Function decorators, class decorators, decorators with arguments,
timing, logging, retry, singleton.
"""

import time
import logging
from functools import wraps


def create_logging_decorator():
    """Create a logging decorator"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            logging.info(f"Calling {func.__name__} with args={args}, kwargs={kwargs}")
            result = func(*args, **kwargs)
            logging.info(f"{func.__name__} returned {result}")
            return result
        return wrapper
    return decorator


def timing_decorator(func):
    """Decorator to measure execution time"""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        end = time.perf_counter()
        print(f"  {func.__name__} took {end - start:.4f} seconds")
        return result
    return wrapper


def retry_decorator(max_attempts=3, delay=1):
    """Decorator to retry failed functions"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            attempts = 0
            while attempts < max_attempts:
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    attempts += 1
                    print(f"  Attempt {attempts} failed: {e}")
                    if attempts == max_attempts:
                        raise
            return None
        return wrapper
    return decorator


def decorators_demo() -> None:
    print("\n" + "=" * 70)
    print("2. DECORATORS")
    print("=" * 70)

    # Basic decorator
    print("\n2.1 Basic Decorator:")
    @timing_decorator
    def slow_function(seconds):
        """A function that takes time to execute"""
        time.sleep(seconds)
        return f"Slept for {seconds} seconds"

    print("  Calling slow_function(0.1):")
    result = slow_function(0.1)
    print(f"  Result: {result}")

    # Decorator with arguments
    print("\n2.2 Decorator with Arguments:")
    logging.getLogger().setLevel(logging.WARNING)

    @create_logging_decorator()
    def add(a, b):
        return a + b

    print("  Calling add(3, 4):")
    result = add(3, 4)
    print(f"  Result: {result}")

    # Class decorators
    print("\n2.3 Class Decorator:")
    def singleton(cls):
        """Decorator to make a class a singleton"""
        instances = {}
        @wraps(cls)
        def get_instance(*args, **kwargs):
            if cls not in instances:
                instances[cls] = cls(*args, **kwargs)
            return instances[cls]
        return get_instance

    @singleton
    class Database:
        def __init__(self):
            self.connection = "connected"

    db1 = Database()
    db2 = Database()
    print(f"  db1 is db2: {db1 is db2}")
    print(f"  Both reference same instance: {id(db1) == id(db2)}")

    # Retry decorator
    print("\n2.4 Retry Decorator:")
    @retry_decorator(max_attempts=3)
    def might_fail(attempt):
        if attempt < 2:
            raise ValueError("Still failing")
        return "Success!"

    print("  Calling might_fail(1):")
    try:
        result = might_fail(1)
        print(f"  Result: {result}")
    except Exception as e:
        print(f"  Failed after retries: {e}")


if __name__ == "__main__":
    decorators_demo()
