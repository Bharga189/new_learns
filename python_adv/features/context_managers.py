# -*- coding: utf-8 -*-
"""
Feature 4: Context Managers
with statement, __enter__/__exit__, contextlib, nested context managers.
"""

from contextlib import contextmanager, suppress, redirect_stdout
import io


class FileHandler:
    """Custom file handler demonstrating __enter__ and __exit__"""

    def __init__(self, filename, mode='r'):
        self.filename = filename
        self.mode = mode
        self.file = None

    def __enter__(self):
        print(f"  Opening file: {self.filename} in {self.mode} mode")
        self.file = open(self.filename, self.mode)
        return self.file

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.file:
            print(f"  Closing file: {self.filename}")
            self.file.close()
        # Return True to suppress exceptions
        return False


@contextmanager
def managed_resource(name):
    """Context manager using decorator"""
    print(f"  Acquiring resource: {name}")
    resource = f"Resource({name})"
    try:
        yield resource
    finally:
        print(f"  Releasing resource: {name}")


def context_managers_demo() -> None:
    print("\n" + "=" * 70)
    print("4. CONTEXT MANAGERS")
    print("=" * 70)

    # Basic file handling with with statement
    print("\n4.1 Basic 'with' Statement:")
    print("  Writing to test file:")
    with open("context_test.txt", "w") as f:
        f.write("Hello, Context Manager!")
    print("  File automatically closed after with block")

    # Custom context manager class
    print("\n4.2 Custom Context Manager Class:")
    print("  Using FileHandler:")
    with FileHandler("context_test.txt", "r") as f:
        content = f.read()
        print(f"  File content: {content}")

    # Context manager with decorator
    print("\n4.3 Context Manager Decorator:")
    print("  Using managed_resource:")
    with managed_resource("Database") as resource:
        print(f"  Using {resource}")

    # Redirect output
    print("\n4.4 Redirect Output:")
    with redirect_stdout(io.StringIO()) as f:
        print("This won't appear")
    print(f"  Captured: {f.getvalue().strip()}")

    # Suppress exceptions
    print("\n4.5 Suppress Exceptions:")
    with suppress(FileNotFoundError):
        open("nonexistent.txt").read()
    print("  Handled missing file silently")

    # Nested context managers
    print("\n4.6 Nested Context Managers:")
    with open("context_test.txt", "r") as f, open("output.txt", "w") as out:
        content = f.read()
        out.write(content.upper())
    print("  Read and wrote file with nested with statements")


if __name__ == "__main__":
    context_managers_demo()
