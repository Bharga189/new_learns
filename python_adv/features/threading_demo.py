# -*- coding: utf-8 -*-
"""
Feature 6b: Threading and Multiprocessing
Threading (threading module), multiprocessing (Process/Queue),
race conditions, locks, concurrent.futures, GIL.
"""

import threading
import time
import os
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor


def worker(name: int, delay: float) -> str:
    """Worker function for threading"""
    print(f"  [{name}] Worker started (PID: {os.getpid()})")
    time.sleep(delay)
    print(f"  [{name}] Worker finished")
    return f"Result from worker {name}"


class Counter:
    """Thread-safe counter using a lock"""

    def __init__(self):
        self.value = 0
        self._lock = threading.Lock()

    def increment(self, steps: int):
        for _ in range(steps):
            with self._lock:
                self.value += 1

    def get_value(self) -> int:
        with self._lock:
            return self.value


def compute_square(n):
    """CPU-bound function for ProcessPoolExecutor"""
    time.sleep(0.3)
    return n * n


def threading_demo() -> None:
    print("\n" + "=" * 70)
    print("6b. THREADING AND MULTITHREADING")
    print("=" * 70)

    # Basic thread
    print("\n1. Basic Thread:")
    def task(name, seconds):
        print(f"  Thread {name} starting")
        time.sleep(seconds)
        print(f"  Thread {name} done")
        return f"{name}_result"

    t1 = threading.Thread(target=task, args=("T1", 1))
    t2 = threading.Thread(target=task, args=("T2", 2))

    print("  Starting threads...")
    t1.start()
    t2.start()

    # Wait for threads to finish
    t1.join()
    t2.join()
    print("  Both threads finished")

    # Thread with return value using queue
    print("\n2. Thread with Return Value:")
    import queue

    result_queue = queue.Queue()

    def task_with_result(name, seconds):
        time.sleep(seconds)
        result_queue.put(f"{name}_result")

    t3 = threading.Thread(target=task_with_result, args=("T3", 1))
    t3.start()
    t3.join()
    result = result_queue.get()
    print(f"  Got result: {result}")

    # Race condition demonstration
    print("\n3. Race Condition:")
    counter = Counter()
    num_threads = 10
    increments_per_thread = 100000

    def increment_counter():
        counter.increment(increments_per_thread)

    threads = []
    for i in range(num_threads):
        t = threading.Thread(target=increment_counter)
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    print(f"  Expected: {num_threads * increments_per_thread}")
    print(f"  Actual:   {counter.get_value()}")

    # Thread-safe counter
    print("\n4. Thread-Safe Counter (with Lock):")
    safe_counter = Counter()
    threads = []

    def increment_safe():
        safe_counter.increment(increments_per_thread)

    for i in range(num_threads):
        t = threading.Thread(target=increment_safe)
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    print(f"  Expected: {num_threads * increments_per_thread}")
    print(f"  Actual:   {safe_counter.get_value()}")

    # ThreadPoolExecutor
    print("\n5. ThreadPoolExecutor:")
    def fetch_url(url):
        time.sleep(0.5)  # Simulate network request
        return f"Data from {url}"

    urls = [f"https://example.com/{i}" for i in range(5)]

    with ThreadPoolExecutor(max_workers=3) as executor:
        results = list(executor.map(fetch_url, urls))

    print(f"  Fetched {len(results)} URLs:")
    for r in results:
        print(f"    {r}")

    # ProcessPoolExecutor (multiprocessing)
    print("\n6. ProcessPoolExecutor:")

    numbers = [1, 2, 3, 4, 5]

    with ProcessPoolExecutor(max_workers=3) as executor:
        results = list(executor.map(compute_square, numbers))

    print(f"  Squares: {results}")

    # GIL explanation
    print("\n7. GIL (Global Interpreter Lock):")
    print("  - Python threads are limited by the GIL")
    print("  - GIL allows only one thread to execute Python bytecode at a time")
    print("  - Threads are useful for I/O-bound tasks (network, files)")
    print("  - For CPU-bound tasks, use multiprocessing (ProcessPoolExecutor)")
    print("  - multiprocessing avoids the GIL by using separate processes")

    # Concurrent.futures summary
    print("\n8. concurrent.futures Summary:")
    print("  - ThreadPoolExecutor: for I/O-bound tasks")
    print("  - ProcessPoolExecutor: for CPU-bound tasks")
    print("  - Both provide a high-level interface for asynchronous execution")


if __name__ == "__main__":
    threading_demo()
