# -*- coding: utf-8 -*-
"""
Feature 6: Collections Module
deque, OrderedDict, defaultdict, namedtuple, ChainMap, Counter.
"""

from collections import (deque, OrderedDict, defaultdict, namedtuple,
                          ChainMap, Counter)


def collections_demo() -> None:
    print("\n" + "=" * 70)
    print("6. COLLECTIONS MODULE")
    print("=" * 70)

    # Deque
    print("\n6.1 deque (Double-ended queue):")
    d = deque([1, 2, 3])
    print(f"  Initial: {d}")

    d.append(4)
    print(f"  After append: {d}")

    d.appendleft(0)
    print(f"  After appendleft: {d}")

    d.pop()
    print(f"  After pop: {d}")

    d.popleft()
    print(f"  After popleft: {d}")

    # Rotate
    d.rotate(1)
    print(f"  After rotate(1): {d}")

    d.rotate(-1)
    print(f"  After rotate(-1): {d}")

    # Maxlen
    print("\n  With maxlen=3:")
    d2 = deque([1, 2, 3], maxlen=3)
    print(f"  Initial: {d2}")
    d2.append(4)
    print(f"  After append(4): {d2} (item removed from opposite end)")

    # OrderedDict
    print("\n6.2 OrderedDict:")
    od = OrderedDict()
    od['a'] = 1
    od['b'] = 2
    od['c'] = 3
    print(f"  OrderedDict: {od}")

    # Regular dict (Python 3.7+) is ordered
    d = {}
    d['a'] = 1
    d['b'] = 2
    d['c'] = 3
    print(f"  Regular dict: {d} (also ordered in Python 3.7+)")

    # defaultdict
    print("\n6.3 defaultdict:")
    dd = defaultdict(list)
    dd['fruits'].append('apple')
    dd['fruits'].append('banana')
    dd['vegetables'].append('carrot')

    print(f"  defaultdict: {dict(dd)}")
    print(f"  Accessing missing key: {dd['dairy']} (auto-created)")

    # namedtuple
    print("\n6.4 namedtuple:")
    Point = namedtuple('Point', ['x', 'y'])
    p = Point(10, 20)
    print(f"  Point created: {p}")
    print(f"  Access by index: {p[0]}, {p[1]}")
    print(f"  Access by name: {p.x}, {p.y}")

    # ChainMap
    print("\n6.5 ChainMap:")
    dict1 = {'a': 1, 'b': 2}
    dict2 = {'b': 3, 'c': 4}
    chain = ChainMap(dict1, dict2)
    print(f"  ChainMap: {chain}")
    print(f"  Chain['a']: {chain['a']} (from dict1)")
    print(f"  Chain['b']: {chain['b']} (from first dict with key)")
    print(f"  Chain['c']: {chain['c']} (from dict2)")

    # Counter
    print("\n6.6 Counter:")
    words = ['apple', 'banana', 'apple', 'cherry', 'banana', 'apple']
    counter = Counter(words)
    print(f"  Counter: {counter}")
    print(f"  Most common: {counter.most_common(2)}")
    print(f"  Sum of counts: {sum(counter.values())}")

    # Set operations
    c1 = Counter(['a', 'b', 'c', 'a'])
    c2 = Counter(['a', 'd', 'e'])
    print(f"  c1: {c1}")
    print(f"  c2: {c2}")
    print(f"  c1 + c2: {c1 + c2}")
    print(f"  c1 - c2: {c1 - c2}")


if __name__ == "__main__":
    collections_demo()
