#!/usr/bin/env python3
"""
Python Basic - Comprehensive Guide to Core Python Features (Modular Edition)

This package contains modular feature files, each covering one Python concept.
Run `main.py` to execute all demonstrations.
"""

# Import all feature modules and run demos
import asyncio
from features import (
    variables_types,
    operators,
    control_flow,
    functions,
    strings,
    lists,
    tuples,
    dictionaries,
    sets,
    file_io,
    error_handling,
    modules,
    list_comprehensions,
    oop_basics,
    oop_advanced,
    advanced_data_structures,
    functional_programming,
    decorators_core,
    decorators_advanced,
    context_managers,
    async_demo,
    testing,
    performance,
    design_patterns_creational,
    design_patterns_behavioral,
    files_and_data,
    best_practices,
    package_structure,
    professional_project_setup,
)

# Also import the old comprehensive version for reference
import python_basic as comprehensive


def main() -> None:
    """Run all demonstrations from modular features."""
    print("=" * 70)
    print("PYTHON BASIC - Comprehensive Guide to Core Python Features")
    print("Modular Edition: All Features in Separate Files")
    print("=" * 70)

    # Run modular feature demos
    variables_types.demo()
    operators.demo()
    control_flow.demo()
    functions.demo()
    strings.demo()
    lists.demo()
    tuples.demo()
    dictionaries.demo()
    sets.demo()
    file_io.demo()
    error_handling.demo()
    modules.demo()
    list_comprehensions.demo()
    oop_basics.demo()
    oop_advanced.demo()
    advanced_data_structures.demo()
    functional_programming.demo()
    decorators_core.demo()
    decorators_advanced.demo()
    context_managers.demo()
    asyncio.run(async_demo.run())
    testing.demo()
    performance.demo()
    design_patterns_creational.demo()
    design_patterns_behavioral.demo()
    files_and_data.demo()
    best_practices.demo()
    package_structure.demo()
    professional_project_setup.demo()

    print("\n" + "=" * 70)
    print("END OF PYTHON BASIC GUIDE")
    print("=" * 70)


if __name__ == "__main__":
    main()
