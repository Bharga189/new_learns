# -*- coding: utf-8 -*-
"""
Feature 15: Advanced Data Structures
deque, OrderedDict, defaultdict, namedtuple, ChainMap, Counter.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("15. ADVANCED DATA STRUCTURES")
    print("---" * 20)

    from collections import (deque, OrderedDict, defaultdict, namedtuple, ChainMap, Counter)

    print("\n--- deque (Double-ended queue) ---")
    d: deque = deque([1, 2, 3])
    print(f"Initial: {d}")
    d.append(4)
    print(f"After append: {d}")
    d.appendleft(0)
    print(f"After appendleft: {d}")
    d.pop()
    print(f"After pop: {d}")
    d.popleft()
    print(f"After popleft: {d}")
    d.rotate(1)
    print(f"After rotate(1): {d}")
    d.rotate(-1)
    print(f"After rotate(-1): {d}")

    print("\nWith maxlen=3:")
    d2: deque = deque([1, 2, 3], maxlen=3)
    print(f"Initial: {d2}")
    d2.append(4)
    print(f"After append(4): {d2} (item removed from opposite end)")

    print("\n--- OrderedDict ---")
    od: OrderedDict = OrderedDict()
    od["a"] = 1
    od["b"] = 2
    od["c"] = 3
    print(f"OrderedDict: {od}")

    d: dict = {}
    d["a"] = 1
    d["b"] = 2
    d["c"] = 3
    print(f"Regular dict: {d} (also ordered in Python 3.7+)")

    print("\n--- defaultdict ---")
    dd: dict = defaultdict(list)
    dd["fruits"].append("apple")
    dd["fruits"].append("banana")
    dd["vegetables"].append("carrot")
    print(f"defaultdict: {dict(dd)}")
    print(f"Accessing missing key: {dd['dairy']} (auto-created)")

    print("\n--- namedtuple ---")
    Point: namedtuple = namedtuple("Point", ["x", "y"])
    p: Point = Point(10, 20)
    print(f"Point created: {p}")
    print(f"Access by index: {p[0]}, {p[1]}")
    print(f"Access by name: {p.x}, {p.y}")

    print("\n--- ChainMap ---")
    dict1: dict = {"a": 1, "b": 2}
    dict2: dict = {"b": 3, "c": 4}
    chain: ChainMap = ChainMap(dict1, dict2)
    print(f"ChainMap: {chain}")
    print(f"Chain['a']: {chain['a']} (from dict1)")
    print(f"Chain['b']: {chain['b']} (from first dict with key)")
    print(f"Chain['c']: {chain['c']} (from dict2)")

    print("\n--- Counter ---")
    words: list = ["apple", "banana", "apple", "cherry", "banana", "apple"]
    counter: Counter = Counter(words)
    print(f"Counter: {counter}")
    print(f"Most common: {counter.most_common(2)}")
    print(f"Sum of counts: {sum(counter.values())}")

    c1: Counter = Counter(["a", "b", "c", "a"])
    c2: Counter = Counter(["a", "d", "e"])
    print(f"c1: {c1}")
    print(f"c2: {c2}")
    print(f"c1 + c2: {c1 + c2}")
    print(f"c1 - c2: {c1 - c2}")
