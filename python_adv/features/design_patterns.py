# -*- coding: utf-8 -*-
"""
Feature 13: Design Patterns
Singleton, Factory, Strategy, Observer, Decorator, Iterator, Context Manager.
"""

from abc import ABC, abstractmethod
from functools import wraps
import functools
from typing import Any, Callable


class Singleton:
    """Singleton pattern - ensures one instance"""
    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance


class DatabaseConnection(Singleton):
    """Database connection - singleton"""

    def __init__(self):
        if not hasattr(self, 'connected'):
            self.connected = True
            print("  Database connected")

    def query(self, query: str) -> str:
        return f"Executing: {query}"


class PaymentProcessor(ABC):
    """Strategy interface"""
    @abstractmethod
    def process(self, amount: float) -> str:
        pass


class CreditCardPayment(PaymentProcessor):
    """Credit card payment strategy"""
    def process(self, amount: float) -> str:
        return f"Processing credit card payment of ${amount:.2f}"


class PayPalPayment(PaymentProcessor):
    """PayPal payment strategy"""
    def process(self, amount: float) -> str:
        return f"Processing PayPal payment of ${amount:.2f}"


class ShoppingCart:
    """Shopping cart with strategy pattern"""
    def __init__(self):
        self.processor: PaymentProcessor = None
        self.items = []

    def set_payment_strategy(self, processor: PaymentProcessor):
        self.processor = processor

    def add_item(self, item: str):
        self.items.append(item)

    def checkout(self, amount: float):
        if self.processor is None:
            raise ValueError("Payment strategy not set")
        result = self.processor.process(amount)
        print(f"  {result}")
        print(f"  Items: {self.items}")


def make_bold(func: Callable) -> Callable:
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        return f"<b>{func(*args, **kwargs)}</b>"
    return wrapper


def make_italic(func: Callable) -> Callable:
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        return f"<i>{func(*args, **kwargs)}</i>"
    return wrapper


def design_patterns_demo() -> None:
    print("\n" + "=" * 70)
    print("13. DESIGN PATTERNS")
    print("=" * 70)

    # Singleton
    print("\n13.1 Singleton Pattern:")
    db1 = DatabaseConnection()
    db2 = DatabaseConnection()
    print(f"  db1 is db2: {db1 is db2}")

    # Strategy pattern
    print("\n13.2 Strategy Pattern:")
    cart = ShoppingCart()
    cart.add_item("Laptop")
    cart.add_item("Mouse")

    cart.set_payment_strategy(CreditCardPayment())
    cart.checkout(999.99)

    cart.set_payment_strategy(PayPalPayment())
    cart.checkout(59.99)

    # Decorator pattern
    print("\n13.3 Decorator Pattern:")
    @make_bold
    @make_italic
    def get_text() -> str:
        return "Hello!"

    print(f"  Decorated text: {get_text()}")


if __name__ == "__main__":
    design_patterns_demo()
