# -*- coding: utf-8 -*-
"""
Feature 9: Metaprogramming
Reflection, inspect module, dynamic imports, decorators with arguments, exec/eval.
"""

import importlib
import inspect


def dynamic_imports_demo() -> None:
    print("\n" + "=" * 70)
    print("9. METAPROGRAMMING")
    print("=" * 70)

    # Reflection
    print("\n9.1 Reflection:")
    class Sample:
        def method(self):
            pass

    obj = Sample()
    print(f"  dir(obj): {len(dir(obj))} attributes/methods")
    print(f"  type(obj): {type(obj)}")
    print(f"  type(Sample): {type(Sample)}")

    print(f"  hasattr(obj, 'method'): {hasattr(obj, 'method')}")
    print(f"  getattr(obj, 'method'): {getattr(obj, 'method')}")

    # Inspect module
    print("\n9.2 Inspect Module:")
    def sample_func(x: int, y: int) -> int:
        return x + y

    print(f"  inspect.signature(sample_func): {inspect.signature(sample_func)}")
    print(f"  inspect.getdoc(sample_func): {inspect.getdoc(sample_func)}")
    print(f"  inspect.getfile(sample_func): {inspect.getfile(sample_func)}")

    # Dynamic imports
    print("\n9.3 Dynamic Imports:")
    # Learn module is available
    try:
        importlib.import_module('collections')
        print("  Successfully imported collections module")
    except ImportError as e:
        print(f"  Import failed: {e}")

    # Import specific function
    collections = importlib.import_module('collections')
    deque = collections.deque
    print(f"  Imported deque: {deque}")

    # Dynamic code execution
    print("\n9.4 Dynamic Code Execution:")
    code = "2 + 3"
    expr = compile(code, '<string>', 'eval')
    result = eval(expr)
    print(f"  eval('2 + 3'): {result}")

    code_block = "x = 10\ny = 20\nprint(f'  x + y = {x + y}')"
    exec_block = compile(code_block, '<string>', 'exec')
    exec(exec_block)


if __name__ == "__main__":
    dynamic_imports_demo()
