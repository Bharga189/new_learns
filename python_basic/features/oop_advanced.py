# -*- coding: utf-8 -*-
"""
Feature 14c: OOP - Special Methods and Protocols
Dunder methods, comparison, containers, descriptors.
"""

from typing import Any


class Product:
    def __init__(self, name: str, price: float, quantity: int = 1) -> None:
        self.name: str = name
        self.price: float = price
        self.quantity: int = quantity

    def __eq__(self, other: Any) -> bool:
        if not isinstance(other, Product):
            return False
        return self.name == other.name

    def __lt__(self, other: Any) -> bool:
        if not isinstance(other, Product):
            return NotImplemented
        return self.price < other.price

    def __len__(self) -> int:
        return self.quantity

    def total_price(self) -> float:
        return self.price * self.quantity

    def __str__(self) -> str:
        return f"Product({self.name}, ${self.price:.2f} x {self.quantity})"


def demo() -> None:
    print("\n" + "---" * 20)
    print("14. BASIC OOP - SPECIAL METHODS")
    print("---" * 20)

    print("\n5. Special Methods (Product):")
    p1: Product = Product("Laptop", 999.99, 2)
    p2: Product = Product("Mouse", 29.99, 5)
    p3: Product = Product("Laptop", 999.99, 1)

    print(f"   p1: {p1}")
    print(f"   p2: {p2}")
    print(f"   p1 == p3 (by name): {p1 == p3}")
    print(f"   p1 < p2 (by price): {p1 < p2}")
    print(f"   p2 < p1: {p2 < p1}")
    print(f"   len(p1): {len(p1)}")
    print(f"   len(p2): {len(p2)}")

    products: list = [p1, p2]
    products.sort()
    print(f"   Sorted by price: {[str(p) for p in products]}")
