# -*- coding: utf-8 -*-
"""
Feature 18: Context Managers
with statement, __enter__/__exit__, contextlib, redirect, suppress, nested.
"""

from contextlib import redirect_stdout
from io import StringIO


class FileHandler:
    def __init__(self, filename: str, mode: str = "r") -> None:
        self.filename: str = filename
        self.mode: str = mode
        self.file = None

    def __enter__(self) -> any:
        print(f"  Opening file: {self.filename} in {self.mode} mode")
        self.file = open(self.filename, self.mode)
        return self.file

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        if self.file:
            print(f"  Closing file: {self.filename}")
            self.file.close()
        return False


from contextlib import contextmanager, redirect_stdout, suppress
from io import StringIO


@contextmanager
def managed_resource(name: str):
    print(f"  Acquiring resource: {name}")
    resource: str = f"Resource({name})"
    try:
        yield resource
    finally:
        print(f"  Releasing resource: {name}")


def demo() -> None:
    print("\n" + "---" * 20)
    print("18. CONTEXT MANAGERS")
    print("---" * 20)

    print("\n1.1 Basic 'with' Statement:")
    print("  Writing to test file:")
    with open("context_test.txt", "w", encoding="utf-8") as f:
        f.write("Hello, Context Manager!")
    print("  File automatically closed after with block")

    print("\n1.2 Custom Context Manager Class:")
    print("  Using FileHandler:")
    with FileHandler("context_test.txt", "r") as f:
        content: str = f.read()
        print(f"  File content: {content}")

    print("\n1.3 Context Manager Decorator:")
    print("  Using managed_resource:")
    with managed_resource("Database") as resource:
        print(f"  Using {resource}")

    print("\n1.4 Redirect Output:")
    with redirect_stdout(StringIO()) as f:
        print("This won't appear")
    print(f"  Captured: {f.getvalue().strip()}")

    print("\n1.5 Suppress Exceptions:")
    with suppress(FileNotFoundError):
        open("nonexistent.txt").read()
    print("  Handled missing file silently")

    print("\n1.6 Nested Context Managers:")
    with open("context_test.txt", "r", encoding="utf-8") as f, \
            open("output.txt", "w", encoding="utf-8") as out:
        content = f.read()
        out.write(content.upper())
    print("  Read and wrote file with nested with statements")

    import os
    os.remove("context_test.txt")
    if os.path.exists("output.txt"):
        os.remove("output.txt")
