#!/usr/bin/env python3
"""
Python Advanced - Comprehensive Guide to Advanced Python Features

This package contains modular feature files, each covering one advanced
Python concept. Run `main.py` to execute all demonstrations.
"""

# Import all feature modules and run demos
import asyncio
from features import (
    advanced_functions,
    decorators,
    generators,
    context_managers,
    advanced_oop,
    collections_module,
    threading_demo,
    type_hints,
    async_demo,
    metaprogramming,
    regex,
    testing_debugging,
    performance,
    design_patterns,
    files_and_data,
)


def main() -> None:
    """Run all demonstrations from modular features."""
    print("=" * 70)
    print("PYTHON ADVANCED - Comprehensive Guide to Advanced Python Features")
    print("Modular Edition: All Features in Separate Files")
    print("=" * 70)

    # Run modular feature demos
    advanced_functions.advanced_functions_demo()
    decorators.decorators_demo()
    generators.generator_demo()
    context_managers.context_managers_demo()
    advanced_oop.advanced_oop_demo()
    collections_module.collections_demo()
    threading_demo.threading_demo()
    type_hints.type_hints_demo()
    asyncio.run(async_demo.async_demo())
    metaprogramming.dynamic_imports_demo()
    regex.regex_demo()
    testing_debugging.testing_demo()
    performance.performance_demo()
    design_patterns.design_patterns_demo()
    files_and_data.files_and_data_demo()

    print("\n" + "=" * 70)
    print("END OF PYTHON ADVANCED GUIDE")
    print("=" * 70)


if __name__ == "__main__":
    main()
