# -*- coding: utf-8 -*-
"""
Feature 4: Functions
No/with/default params, *args/**kwargs, return multiple values, lambda, type hints.
"""

def no_params() -> str:
    return "Hello from no_params!"


def with_params(name: str, age: int) -> str:
    return f"{name} is {age} years old"


def default_params(name: str = "Guest", greeting: str = "Hello") -> str:
    return f"{greeting}, {name}!"


def variable_args(*args) -> any:
    print(f"  args: {args} (type: {type(args).__name__})")
    return sum(args)


def variable_kwargs(**kwargs) -> any:
    print(f"  kwargs: {kwargs} (type: {type(kwargs).__name__})")
    return kwargs


def mixed_params(a: int, b: int, *args, c: int = 10, **kwargs) -> int:
    print(f"  a={a}, b={b}, args={args}, c={c}, kwargs={kwargs}")
    return a + b + sum(args) + c


def return_multiple() -> tuple:
    name: str = "Alice"
    age: int = 30
    city: str = "New York"
    return name, age, city


def lambda_demo() -> None:
    square = lambda x: x ** 2
    add = lambda x, y: x + y
    print(f"  square(5) = {square(5)}")
    print(f"  add(3, 4) = {add(3, 4)}")

    numbers: list = [1, 2, 3, 4, 5]
    squared = list(map(lambda x: x ** 2, numbers))
    evens = list(filter(lambda x: x % 2 == 0, numbers))
    sorted_by_length = sorted(["apple", "banana", "kiwi"], key=lambda x: len(x))
    print(f"  Squared: {squared}")
    print(f"  Evens: {evens}")
    print(f"  Sorted by length: {sorted_by_length}")


def demo() -> None:
    print("\n" + "---" * 20)
    print("4. FUNCTIONS")
    print("---" * 20)

    print("\nNo params:")
    print(f"  {no_params()}")
    print("\nWith params:")
    print(f"  {with_params('Bob', 25)}")
    print("\nDefault params:")
    print(f"  {default_params()}")
    print(f"  {default_params('Alice')}")
    print(f"  {default_params('Alice', 'Hi')}")
    print(f"  {default_params(greeting='Hey')}")
    print("\nVariable args:")
    result = variable_args(1, 2, 3, 4, 5)
    print(f"  Sum: {result}")
    print("\nVariable kwargs:")
    result = variable_kwargs(name="Charlie", age=35)
    print(f"  Result: {result}")
    print("\nMixed params:")
    result = mixed_params(1, 2, 3, 4, 5, c=100, x=1, y=2)
    print(f"  Result: {result}")
    print("\nReturn multiple values:")
    name, age, city = return_multiple()
    print(f"  name={name}, age={age}, city={city}")
    lambda_demo()
