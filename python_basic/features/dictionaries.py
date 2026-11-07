# -*- coding: utf-8 -*-
"""
Feature 8: Dictionaries
Creation, access, methods, comprehensions, counting/grouping patterns.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("8. DICTIONARIES")
    print("---" * 20)

    empty_dict: dict = {}
    dict_literal: dict = {"name": "Alice", "age": 30, "city": "New York"}
    dict_constructor: dict = dict(name="Bob", age=25, city="Boston")

    print(f"Empty dict: {empty_dict}")
    print(f"Literal: {dict_literal}")
    print(f"Constructor: {dict_constructor}")

    print(f"\ndict['name']: {dict_literal['name']}")
    print(f"get('age'): {dict_literal.get('age')}")
    print(f"get('country', 'Unknown'): {dict_literal.get('country', 'Unknown')}")

    print("\n--- Modifying Dictionaries ---")
    person: dict = {"name": "Alice", "age": 30}
    print(f"Original: {person}")

    person["age"] = 31
    print(f"After update: {person}")

    person["city"] = "New York"
    print(f"After add: {person}")

    removed: any = person.pop("city")
    print(f"After pop 'city': {person}, removed: {removed}")

    print("\n--- Dictionary Methods ---")
    data: dict = {"a": 1, "b": 2, "c": 3}
    print(f"Keys: {list(data.keys())}")
    print(f"Values: {list(data.values())}")
    print(f"Items: {list(data.items())}")

    print("\nIterating:")
    for key in data:
        print(f"  {key}: {data[key]}")

    for key, value in data.items():
        print(f"  {key} = {value}")

    print("\n--- Dictionary Operations ---")
    dict1: dict = {"a": 1, "b": 2}
    dict2: dict = {"c": 3, "d": 4}
    merged: dict = {**dict1, **dict2}
    print(f"Merged: {merged}")

    print(f"'a' in dict1: {'a' in dict1}")
    print(f"1 in dict1.values(): {1 in dict1.values()}")

    print("\n--- Dictionary Comprehensions ---")
    numbers: list = list(range(5))
    squares_dict: dict = {x: x ** 2 for x in numbers}
    print(f"Squares dict: {squares_dict}")

    even_squares: dict = {k: v for k, v in squares_dict.items() if v % 2 == 0}
    print(f"Even squares: {even_squares}")

    print("\n--- Common Patterns ---")
    words: list = ["apple", "banana", "apple", "cherry", "banana", "apple"]
    word_count: dict = {}
    for word in words:
        word_count[word] = word_count.get(word, 0) + 1
    print(f"Word count: {word_count}")

    people: list = [
        {"name": "Alice", "age": 30},
        {"name": "Bob", "age": 25},
        {"name": "Charlie", "age": 30}
    ]
    by_age: dict = {}
    for person in people:
        age = person["age"]
        by_age.setdefault(age, []).append(person["name"])
    print(f"Grouped by age: {by_age}")
