# Python Advanced

A comprehensive guide to advanced Python programming features with detailed explanations and examples.

## Overview

This project covers advanced Python concepts that build upon the basics. Each section is now a separate module under `features/` for easy navigation, reuse, and testing.

## Topics Covered

| # | Feature file | Topic |
|---|---|---|
| 1 | `advanced_functions.py` | First-class functions, closures, higher-order functions, partials |
| 2 | `decorators.py` | Function/class decorators, decorators with arguments, retry, singleton |
| 3 | `generators.py` | Iterator protocol, generators, generator expressions, `yield` |
| 4 | `context_managers.py` | `with`, `__enter__`/`__exit__`, `contextlib`, nested/async context managers |
| 5 | `advanced_oop.py` | Multiple inheritance, MRO, ABC, class/static methods, properties, descriptors, metaclasses, dataclasses, mixins |
| 6 | `collections_module.py` | `deque`, `OrderedDict`, `defaultdict`, `namedtuple`, `ChainMap`, `Counter` |
| 6b | `threading_demo.py` | Threading, locks, race conditions, `ThreadPoolExecutor`, `ProcessPoolExecutor`, GIL |
| 7 | `type_hints.py` | PEP 484, generics, `Union`/`Optional`, type aliases, `Protocol` |
| 8 | `async_demo.py` | `async`/`await`, `asyncio`, coroutines, tasks, `gather`, async context managers |
| 9 | `metaprogramming.py` | Reflection, `inspect`, dynamic imports, `eval`/`exec`, decorators with arguments |
| 10 | `regex.py` | `re` module, pattern matching, groups, lookahead/lookbehind, `re.sub`/`re.split` |
| 11 | `testing_debugging.py` | `unittest`, `pytest`, mocking, `assert`, logging, `pdb` |
| 12 | `performance.py` | `cProfile`, profiling, caching with `lru_cache`, list comp vs loop |
| 13 | `design_patterns.py` | Singleton, Factory, Strategy, Observer, Decorator, Iterator |
| 14 | `files_and_data.py` | `pathlib`, JSON, CSV, Pickle, ConfigParser, environment variables |

## How to Run

```bash
cd python_adv
python main.py
```

## Requirements

- Python 3.6+ (for f-strings)
- Python 3.7+ (for dataclasses)
- Python 3.10+ (for `match`/`case` pattern matching)
