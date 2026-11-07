# -*- coding: utf-8 -*-
"""
Feature 12: Modules and Imports
Standard library: math, datetime, time, collections, sys.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("12. MODULES AND IMPORTS")
    print("---" * 20)

    import math
    import time
    from datetime import datetime, date, timedelta
    from collections import Counter, defaultdict, namedtuple
    import sys

    print("\n--- math module ---")
    print(f"math.pi: {math.pi}")
    print(f"math.e: {math.e}")
    print(f"math.sqrt(16): {math.sqrt(16)}")
    print(f"math.pow(2, 10): {math.pow(2, 10)}")
    print(f"math.floor(3.7): {math.floor(3.7)}")
    print(f"math.ceil(3.2): {math.ceil(3.2)}")
    print(f"math.sin(math.pi/2): {math.sin(math.pi/2)}")
    print(f"math.log10(100): {math.log10(100)}")

    print("\n--- datetime module ---")
    now: datetime = datetime.now()
    print(f"Current datetime: {now}")
    print(f"Date only: {now.date()}")
    print(f"Time only: {now.time()}")

    today: date = date.today()
    print(f"Today: {today}")
    print(f"Tomorrow: {today + timedelta(days=1)}")

    birth_date: date = date(1990, 5, 15)
    age: int = today.year - birth_date.year
    print(f"Age from birth date: {age}")

    print("\n--- time module ---")
    start: float = time.perf_counter()
    time.sleep(0.1)
    elapsed: float = time.perf_counter() - start
    print(f"Elapsed time (100ms sleep): {elapsed:.3f}s")

    print("\n--- collections module ---")
    words: list = ["apple", "banana", "apple", "cherry", "banana", "apple"]
    word_count: Counter = Counter(words)
    print(f"Counter: {word_count}")
    print(f"Most common: {word_count.most_common(2)}")

    dd: dict = defaultdict(list)
    dd["fruits"].append("apple")
    dd["fruits"].append("banana")
    dd["vegetables"].append("carrot")
    print(f"defaultdict: {dict(dd)}")

    Point: namedtuple = namedtuple("Point", ["x", "y"])
    p: Point = Point(10, 20)
    print(f"namedtuple Point: x={p.x}, y={p.y}")

    print("\n--- sys module ---")
    print(f"Python version: {sys.version}")
    print(f"Executable: {sys.executable}")
    print(f"Platform: {sys.platform}")
    print(f"Path entries: {len(sys.path)}")

    print(f"\nCurrent module name: {__name__}")
