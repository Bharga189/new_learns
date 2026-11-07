# -*- coding: utf-8 -*-
"""
Feature 5: Strings
Quote styles, f-strings, string methods, slicing, raw strings.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("5. STRINGS")
    print("---" * 20)

    s1 = 'Single quotes'
    s2 = "Double quotes"
    s3 = '''Triple single quotes
    for multi-line'''
    s4 = """Triple double quotes
    for multi-line"""

    print(f"s1: {s1}")
    print(f"s2: {s2}")
    print(f"Multi-line:\n{s3}")

    name: str = "Alice"
    age: int = 30
    print(f"\nf-string example: My name is {name} and I'm {age}")
    print(f"Expression in f-string: 2 + 3 = {2 + 3}")
    print(f"Formatted number: {3.14159:.2f}")
    print(f"Formatted number: {1_000_000:,}")
    print(f"Multiline f-string:\n  Hi {name},\n  Welcome to Python!")

    path = r"C:\Users\Alice\Documents"
    print(f"\nRaw string (path): {path}")

    text: str = "  Hello, World!  "
    print(f"\nOriginal: '{text}'")
    print(f"strip(): '{text.strip()}'")
    print(f"lower(): '{text.lower()}'")
    print(f"upper(): '{text.upper()}'")
    print(f"capitalize(): '{text.capitalize()}'")
    print(f"title(): '{text.title()}'")
    print(f"replace('World', 'Python'): '{text.replace('World', 'Python')}'")

    sentence: str = "The quick brown fox jumps over the lazy dog"
    print(f"\nFind 'fox': position {sentence.find('fox')}")
    print(f"Count 'the' (case insensitive): {sentence.lower().count('the')}")
    print(f"Starts with 'The': {sentence.startswith('The')}")
    print(f"Ends with 'dog': {sentence.endswith('dog')}")

    word: str = "Python"
    print(f"\nWord: {word}")
    print(f"word[0]: {word[0]}")
    print(f"word[-1]: {word[-1]}")
    print(f"word[0:3]: {word[0:3]}")
    print(f"word[3:]: {word[3:]}")
    print(f"word[:3]: {word[:3]}")
    print(f"word[::-1]: {word[::-1]}")

    print("\n--- String Building ---")
    parts = ["Hello", "world", "from", "Python"]
    result = " ".join(parts)
    print(f"Joined: {result}")
    print(f"Upper first: {result[0].upper() + result[1:]}")
