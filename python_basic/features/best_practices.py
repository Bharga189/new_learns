# -*- coding: utf-8 -*-
"""
Feature 24: Best Practices and Code Quality
PEP 8, readability, documentation, error handling, security, testing.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("24. BEST PRACTICES AND CODE QUALITY")
    print("---" * 20)

    print("\n--- Code Style (PEP 8) ---")
    print("  - Use 4 spaces for indentation")
    print("  - Limit lines to 79 characters")
    print("  - Use descriptive variable names")
    print("  - Use docstrings for modules, functions, and classes")
    print("  - Use type hints for function signatures")

    print("\n--- Readability ---")
    total_price: float = 100.0
    tax_rate: float = 0.08
    tax_amount: float = total_price * tax_rate

    t: float = 100.0
    r: float = 0.08
    a: float = t * r

    print(f"  Total with tax: ${total_price + tax_amount:.2f}")
    print(f"  Good: {total_price + tax_amount:.2f}, Bad #{t * r:.2f}")

    print("\n--- Documentation ---")
    print("  - Module docstring at the top")
    print("  - Function docstrings for all functions")
    print("  - Class docstrings for all classes")

    print("\n--- Error Handling Best Practices ---")
    print("  1. Catch specific exceptions, not bare except")
    print("  2. Use finally for cleanup")
    print("  3. Don't suppress exceptions without reason")
    print("  4. Use raise to re-raise or raise new exceptions")
    print("  5. Create custom exceptions for application-specific errors")

    print("\n--- Testing Best Practices ---")
    print("  1. Write tests first (TDD)")
    print("  2. Test edge cases")
    print("  3. Keep tests independent")
    print("  4. Use descriptive test names")
    print("  5. Mock external dependencies")

    print("\n--- Performance Best Practices ---")
    print("  1. Use appropriate data structures")
    print("  2. Use generators for large data")
    print("  3. List comprehensions are faster than loops")
    print("  4. Use built-in functions")
    print("  5. Use local variables (faster than globals)")

    print("\n--- Security Best Practices ---")
    print("  1. Never use eval() on untrusted input")
    print("  2. Use parameterized queries for SQL")
    print("  3. Validate and sanitize user input")
    print("  4. Use HTTPS for secure communication")
    print("  5. Keep dependencies updated")

    print("\n--- Version Control Best Practices ---")
    print("  1. Write meaningful commit messages")
    print("  2. Use feature branches")
    print("  3. Keep commits atomic")
    print("  4. Write good README files")
    print("  5. Use .gitignore for sensitive files")
