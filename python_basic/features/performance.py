# -*- coding: utf-8 -*-
"""
Feature 21: Performance and Optimization
List comprehension vs loop, lru_cache, cProfile, generators.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("21. PERFORMANCE AND OPTIMIZATION")
    print("---" * 20)

    import cProfile
    import time

    print("\n2.1 List Comprehension vs Loop:")
    numbers: list = list(range(1000000))

    start: float = time.perf_counter()
    result_loop: list = []
    for n in numbers:
        result_loop.append(n ** 2)
    loop_time: float = time.perf_counter() - start

    start = time.perf_counter()
    result_comp: list = [n ** 2 for n in numbers]
    comp_time: float = time.perf_counter() - start

    print(f"  Loop: {loop_time:.4f}s")
    print(f"  List comprehension: {comp_time:.4f}s")
    print(f"  Speedup: {loop_time / comp_time:.2f}x")

    print("\n2.2 Caching with lru_cache:")
    from functools import lru_cache

    @lru_cache(maxsize=128)
    def fibonacci(n: int) -> int:
        if n < 2:
            return n
        return fibonacci(n - 1) + fibonacci(n - 2)

    start = time.perf_counter()
    fib_30: int = fibonacci(30)
    fib_time: float = time.perf_counter() - start
    print(f"  fibonacci(30) = {fib_30}")
    print(f"  Time: {fib_time:.4f}s")

    print("\n2.3 Profiling with cProfile:")
    def slow_function() -> int:
        total: int = 0
        for i in range(1000):
            total += i ** 2
        return total

    pr: cProfile.Profile = cProfile.Profile()
    pr.enable()
    slow_function()
    pr.disable()

    print("  Profile results:")
    pr.print_stats(sort="cumulative")

    print("\n2.4 Memory-Efficient Generator:")
    def large_range(n: int):
        num: int = 0
        while num < n:
            yield num
            num += 1

    import itertools
    gen: generator = large_range(1000000)
    print(f"  Generator created: {gen}")
    print(f"  First 5 values: {list(itertools.islice(gen, 5))}")
