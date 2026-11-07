# -*- coding: utf-8 -*-
"""
Feature 3: Control Flow
If/elif/else, for/while loops, break/continue/pass, match statement.
"""

def demo() -> None:
    print("\n" + "=" * 70)
    print("3. CONTROL FLOW")
    print("=" * 70)

    print("\n--- If/Elif/Else ---")
    score = 85
    if score >= 90:
        grade = "A"
    elif score >= 80:
        grade = "B"
    elif score >= 70:
        grade = "C"
    elif score >= 60:
        grade = "D"
    else:
        grade = "F"
    print(f"Score {score} = Grade {grade}")

    print("\n--- Match/Case Statement ---")
    match grade:
        case "A":
            print("Excellent!")
        case "B":
            print("Good job!")
        case "C":
            print("You passed")
        case "D":
            print("Below average")
        case "F":
            print("Failed")
        case _:
            print("Invalid grade")

    print("\n--- For Loop ---")
    fruits = ["apple", "banana", "cherry"]
    for fruit in fruits:
        print(f"  I like {fruit}")

    print("\nCounting 1 to 5:")
    for i in range(1, 6):
        print(f"  {i}", end=" ")
    print()

    print("\nWith enumerate:")
    for index, fruit in enumerate(fruits):
        print(f"  [{index}] {fruit}")

    print("\nWith zip:")
    colors = ["red", "yellow", "orange"]
    for fruit, color in zip(fruits, colors):
        print(f"  {fruit} is {color}")

    print("\nWith dict items:")
    person = {"name": "Alice", "age": 30, "city": "New York"}
    for key, value in person.items():
        print(f"  {key}: {value}")

    print("\n--- While Loop ---")
    count = 0
    while count < 5:
        print(f"  Count: {count}")
        count += 1

    print("\n--- Break and Continue ---")
    for i in range(10):
        if i == 3:
            print(f"  Skipping {i}")
            continue
        if i == 7:
            print(f"  Breaking at {i}")
            break
        print(f"  Processing {i}")

    print("\n--- Pass Statement ---")
    def placeholder_function():
        pass

    print("  Placeholder function defined (does nothing)")

    print("\n--- Assert Statement ---")
    x = 5
    assert x > 0, "x must be positive"
    print(f"  Assertion passed: x = {x}")
