# -*- coding: utf-8 -*-
"""
Feature 6: Lists
Creation, methods, indexing/slicing, comprehensions, nested lists.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("6. LISTS")
    print("---" * 20)

    empty_list: list = []
    list_with_values: list = [1, 2, 3, 4, 5]
    mixed_list: list = [1, "hello", 3.14, True, [1, 2]]

    print(f"Empty list: {empty_list}")
    print(f"List with values: {list_with_values}")
    print(f"Mixed list: {mixed_list}")

    numbers: list = list(range(10))
    print(f"\nList: {numbers}")
    print(f"numbers[0]: {numbers[0]}")
    print(f"numbers[-1]: {numbers[-1]}")
    print(f"numbers[2:5]: {numbers[2:5]}")
    print(f"numbers[::2]: {numbers[::2]}")
    print(f"numbers[::-1]: {numbers[::-1]}")

    print("\n--- Modifying Lists ---")
    fruits: list = ["apple", "banana", "cherry"]
    print(f"Original: {fruits}")

    fruits.append("date")
    print(f"After append: {fruits}")

    fruits.insert(1, "apricot")
    print(f"After insert at 1: {fruits}")

    fruits.extend(["elderberry", "fig"])
    print(f"After extend: {fruits}")

    removed = fruits.pop()
    print(f"After pop: {fruits}, removed: {removed}")

    fruits.remove("banana")
    print(f"After remove 'banana': {fruits}")

    print("\n--- List Methods ---")
    nums: list = [3, 1, 4, 1, 5, 9, 2, 6]
    print(f"List: {nums}")
    print(f"index(5): {nums.index(5)}")
    print(f"count(1): {nums.count(1)}")
    nums.sort()
    print(f"After sort: {nums}")
    nums.reverse()
    print(f"After reverse: {nums}")
    nums_copy: list = nums.copy()
    print(f"Copy: {nums_copy}")
    nums.clear()
    print(f"After clear: {nums}")

    print("\n--- List Operations ---")
    list1: list = [1, 2, 3]
    list2: list = [4, 5, 6]
    print(f"list1 + list2 = {list1 + list2}")
    print(f"list1 * 3 = {list1 * 3}")
    print(f"2 in list1: {2 in list1}")
    print(f"len(list1): {len(list1)}")

    print("\n--- List Comprehensions ---")
    numbers = list(range(10))

    squares: list = [x ** 2 for x in numbers]
    print(f"Squares: {squares}")

    evens: list = [x for x in numbers if x % 2 == 0]
    odds: list = [x for x in numbers if x % 2 != 0]
    print(f"Evens: {evens}")
    print(f"Odds: {odds}")

    labels: list = ["even" if x % 2 == 0 else "odd" for x in numbers]
    print(f"Labels: {labels}")

    matrix: list = [[i * j for j in range(3)] for i in range(3)]
    print(f"Matrix: {matrix}")

    print("\n--- Flattening Nested Lists ---")
    nested: list = [[1, 2, 3], [4, 5], [6, 7, 8, 9]]
    flat: list = [item for sublist in nested for item in sublist]
    print(f"Flattened: {flat}")

    print("\n--- String Operations in Comprehensions ---")
    words: list = ["hello", "world", "python", "programming"]
    upper_words: list = [word.upper() for word in words]
    word_lengths: list = [len(word) for word in words]
    print(f"Upper: {upper_words}")
    print(f"Lengths: {word_lengths}")
