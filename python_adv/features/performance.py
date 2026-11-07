# -*- coding: utf-8 -*-
"""
Feature 12: Performance and Optimization
cProfile, time complexity, memory profiling, lru_cache, list comprehension vs loop.
"""

import cProfile
import time
import functools


def performance_demo() -> None:
    print("\n" + "=" * 70)
    print("12. PERFORMANCE AND OPTIMIZATION")
    print("=" * 70)

    # List comprehension vs loop
    print("\n12.1 List Comprehension vs Loop:")
    numbers = range(1000000)

    # Using loop
    start = time.perf_counter()
    result_loop = []
    for n in numbers:
        result_loop.append(n ** 2)
    loop_time = time.perf_counter() - start

    # Using list comprehension
    start = time.perf_counter()
    result_comp = [n ** 2 for n in numbers]
    comp_time = time.perf_counter() - start

    print(f"  Loop: {loop_time:.4f}s")
    print(f"  List comprehension: {comp_time:.4f}s")
    print(f"  Speedup: {loop_time / comp_time:.2f}x")

    # Cache with lru_cache
    print("\n12.2 Caching with lru_cache:")
    @functools.lru_cache(maxsize=128)
    def fibonacci(n: int) -> int:
        if n < 2:
            return n
        return fibonacci(n - 1) + fibonacci(n - 2)

    start = time.perf_counter()
    fib_30 = fibonacci(30)
    fib_time = time.perf_counter() - start
    print(f"  fibonacci(30) = {fib_30}")
    print(f"  Time: {fib_time:.4f}s")

    # Profile code
    print("\n12.3 Profiling with cProfile:")
    def slow_function() -> int:
        total = 0
        for i in range(1000):
            total += i ** 2
        return total

    pr = cProfile.Profile()
    pr.enable()
    slow_function()
    pr.disable()

    print("  Profile results:")
    pr.print_stats(sort='cumulative')


if __name__ == "__main__":
    performance_demo()
