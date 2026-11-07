# -*- coding: utf-8 -*-
"""
Feature 17b: Decorators - Advanced Patterns
Class decorators, decorators with arguments, retry, singleton.
"""

from functools import wraps


def singleton(cls: type) -> callable:
    instances: dict = {}

    @wraps(cls)
    def get_instance(*args, **kwargs):
        if cls not in instances:
            instances[cls] = cls(*args, **kwargs)
        return instances[cls]
    return get_instance


def retry_decorator(max_attempts: int = 3, delay: int = 1) -> callable:
    def decorator(func: callable) -> callable:
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


def demo() -> None:
    print("\n" + "---" * 20)
    print("17b. DECORATORS - ADVANCED PATTERNS")
    print("---" * 20)

    print("\n1.3 Class Decorator:")
    @singleton
    class Database:
        def __init__(self) -> None:
            self.connection: str = "connected"

    db1: Database = Database()
    db2: Database = Database()
    print(f"  db1 is db2: {db1 is db2}")
    print(f"  Both reference same instance: {id(db1) == id(db2)}")

    print("\n1.4 Retry Decorator:")

    @retry_decorator(max_attempts=3)
    def might_fail(attempt: int) -> str:
        if attempt < 2:
            raise ValueError("Still failing")
        return "Success!"

    print("  Calling might_fail(1):")
    try:
        result: str = might_fail(1)
        print(f"  Result: {result}")
    except Exception as e:
        print(f"  Failed after retries: {e}")
