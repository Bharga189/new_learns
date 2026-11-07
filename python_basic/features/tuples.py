# -*- coding: utf-8 -*-
"""
Feature 7: Tuples
Creation, immutability, unpacking, dictionary keys, use cases.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("7. TUPLES")
    print("---" * 20)

    empty_tuple: tuple = ()
    single_tuple: tuple = (1,)
    multi_tuple: tuple = (1, 2, 3, "hello", 3.14)
    mixed_tuple: tuple = (1, "hello", [1, 2], {"key": "value"})

    print(f"Empty tuple: {empty_tuple}")
    print(f"Single tuple: {single_tuple}")
    print(f"Multi tuple: {multi_tuple}")
    print(f"Mixed tuple: {mixed_tuple}")

    print(f"\nLength: {len(multi_tuple)}")
    print(f"First element: {multi_tuple[0]}")
    print(f"Last element: {multi_tuple[-1]}")
    print(f"Slicing [1:3]: {multi_tuple[1:3]}")
    print(f"Count of 1: {multi_tuple.count(1)}")
    print(f"Index of 'hello': {multi_tuple.index('hello')}")

    print("\n--- Immutability ---")
    try:
        multi_tuple[0] = 100
    except TypeError as e:
        print(f"Can't modify tuple: {e}")

    print("\n--- Tuple Unpacking ---")
    coordinates: tuple = (10, 20, 30)
    x, y, z = coordinates
    print(f"Unpacked: x={x}, y={y}, z={z}")

    a, b = 5, 10
    a, b = b, a
    print(f"After swap: a={a}, b={b}")

    person: tuple = ("Alice", 30, ("New York", "USA"))
    name, age, (city, country) = person
    print(f"Name: {name}, Age: {age}, City: {city}, Country: {country}")

    print("\n--- Tuple Use Cases ---")
    locations: dict = {
        (40.7128, -74.0060): "New York",
        (34.0522, -118.2437): "Los Angeles",
        (51.5074, -0.1278): "London"
    }
    print(f"Location lookup: {locations[(40.7128, -74.0060)]}")

    def get_stats(numbers: list) -> tuple:
        return min(numbers), max(numbers), sum(numbers) / len(numbers)

    result: tuple = get_stats([1, 2, 3, 4, 5])
    print(f"Stats (min, max, avg): {result}")
