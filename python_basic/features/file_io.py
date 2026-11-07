# -*- coding: utf-8 -*-
"""
Feature 10: File I/O
Reading, writing, appending, pathlib operations, context managers.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("10. FILE I/O")
    print("---" * 20)

    import pathlib
    current_dir: pathlib.Path = pathlib.Path.cwd()
    test_file: pathlib.Path = current_dir / "test_file.txt"

    print(f"Writing to: {test_file}")
    with open(test_file, "w", encoding="utf-8") as f:
        f.write("Line 1: Hello, World!\n")
        f.write("Line 2: Python is awesome\n")
        f.write("Line 3: File I/O is easy\n")

    print("\nReading entire file:")
    with open(test_file, "r", encoding="utf-8") as f:
        content: str = f.read()
        print(content)

    print("Reading line by line:")
    with open(test_file, "r", encoding="utf-8") as f:
        for i, line in enumerate(f, 1):
            print(f"  Line {i}: {line.strip()}")

    print("\nReading all lines as list:")
    with open(test_file, "r", encoding="utf-8") as f:
        lines: list = f.readlines()
        print(f"Lines: {lines}")

    print("\nAppending to file:")
    with open(test_file, "a", encoding="utf-8") as f:
        f.write("Line 4: Appended content\n")

    print("\nReading after append:")
    with open(test_file, "r", encoding="utf-8") as f:
        print(f.read())

    print("\n--- Path Operations ---")
    print(f"Exists: {test_file.exists()}")
    print(f"Is file: {test_file.is_file()}")
    print(f"Name: {test_file.name}")
    print(f"Stem: {test_file.stem}")
    print(f"Suffix: {test_file.suffix}")
    print(f"Parent: {test_file.parent}")

    test_file.unlink()
    print(f"\nDeleted: {test_file.exists()}")
