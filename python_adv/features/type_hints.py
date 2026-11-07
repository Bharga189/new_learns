# -*- coding: utf-8 -*-
"""
Feature 7: Type Hints and Static Analysis
PEP 484, Generic types, Union/Optional, Type aliases, Protocol, typing module.
"""

from typing import (List, Dict, Tuple, Optional, Union, Callable, TypeVar,
                    Generic, Protocol)


# Type aliases
UserId = str
UserRole = str

# Generic types
T = TypeVar('T')
K = TypeVar('K')
V = TypeVar('V')


class Container(Generic[T]):
    """Generic container class"""
    def __init__(self, value: T):
        self.value = value

    def get(self) -> T:
        return self.value


def process_users(users: List[Dict[str, Union[str, int]]]) -> Dict[str, int]:
    """Process a list of users and return user count per role"""
    result: Dict[str, int] = {}
    for user in users:
        role = user.get('role', 'unknown')
        result[role] = result.get(role, 0) + 1
    return result


def call_with_args(func: Callable[[int, str], bool], arg1: int, arg2: str) -> bool:
    """Function that takes a callable"""
    return func(arg1, arg2)


def type_hints_demo() -> None:
    print("\n" + "=" * 70)
    print("7. TYPE HINTS AND STATIC ANALYSIS")
    print("=" * 70)

    # Basic type hints
    print("\n7.1 Basic Type Hints:")
    def greet(name: str, age: int) -> str:
        return f"Hello, {name}. You are {age} years old."

    print(f"  greet('Alice', 30): {greet('Alice', 30)}")

    # Optional and Union
    print("\n7.2 Optional and Union:")
    def find_user(users: List[str], name: str) -> Optional[str]:
        for user in users:
            if user == name:
                return user
        return None

    users = ['Alice', 'Bob', 'Charlie']
    print(f"  find_user(users, 'Bob'): {find_user(users, 'Bob')}")
    print(f"  find_user(users, 'David'): {find_user(users, 'David')}")

    # Dict and List types
    print("\n7.3 Dict and List Types:")
    scores: Dict[str, int] = {'Alice': 95, 'Bob': 87, 'Charlie': 92}
    names: List[str] = ['Alice', 'Bob', 'Charlie']
    coordinates: Tuple[float, float] = (3.14, 2.71)

    print(f"  scores: {scores}")
    print(f"  names: {names}")
    print(f"  coordinates: {coordinates}")

    # Advanced type hints
    print("\n7.4 Advanced Type Hints:")
    # Protocol for structural subtyping
    class Shape(Protocol):
        def area(self) -> float:
            ...

    class Rectangle:
        def __init__(self, width: float, height: float):
            self.width = width
            self.height = height

        def area(self) -> float:
            return self.width * self.height

    class Circle:
        def __init__(self, radius: float):
            self.radius = radius

        def area(self) -> float:
            return 3.14159 * self.radius ** 2

    def print_area(shape: Shape) -> None:
        print(f"  Area: {shape.area():.2f}")

    rect = Rectangle(5, 3)
    circ = Circle(4)

    print("  Rectangle area:")
    print_area(rect)
    print("  Circle area:")
    print_area(circ)

    # Callable types
    print("\n7.5 Callable Types:")
    def add(x: int, y: int) -> int:
        return x + y

    def multiply(x: int, y: int) -> int:
        return x * y

    print(f"  call_with_args(add, 3, 4): {call_with_args(add, 3, 4)}")
    print(f"  call_with_args(multiply, 3, 4): {call_with_args(multiply, 3, 4)}")


if __name__ == "__main__":
    type_hints_demo()
