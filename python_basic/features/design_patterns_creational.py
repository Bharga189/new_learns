# -*- coding: utf-8 -*-
"""
Feature 22a: Design Patterns - Creational
Singleton and Factory patterns.
"""





class Singleton:
    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance


class DatabaseConnection(Singleton):
    def __init__(self) -> None:
        if not hasattr(self, "connected"):
            self.connected: bool = True
            print("  Database connected")

    def query(self, query: str) -> str:
        return f"Executing: {query}"


class PaymentProcessor:
    def process(self, amount: float) -> str:
        pass


class CreditCardPayment(PaymentProcessor):
    def process(self, amount: float) -> str:
        return f"Processing credit card payment of ${amount:.2f}"


class PayPalPayment(PaymentProcessor):
    def process(self, amount: float) -> str:
        return f"Processing PayPal payment of ${amount:.2f}"


def factory_demo(price: float, method: str = "card") -> str:
    """Factory: create the right payment processor."""
    if method == "paypal":
        processor = PayPalPayment()
    else:
        processor = CreditCardPayment()
    return processor.process(amount=price)


class ShoppingCart:
    def __init__(self) -> None:
        self.processor: PaymentProcessor = None
        self.items: list = []

    def set_payment_strategy(self, processor: PaymentProcessor) -> None:
        self.processor = processor

    def add_item(self, item: str) -> None:
        self.items.append(item)

    def checkout(self, amount: float) -> None:
        if self.processor is None:
            raise ValueError("Payment strategy not set")
        result: str = self.processor.process(amount)
        print(f"  {result}")
        print(f"  Items: {self.items}")


def demo() -> None:
    print("\n" + "---" * 20)
    print("22a. DESIGN PATTERNS - CREATIONAL")
    print("---" * 20)

    print("\n2.1 Singleton Pattern:")
    db1: DatabaseConnection = DatabaseConnection()
    db2: DatabaseConnection = DatabaseConnection()
    print(f"  db1 is db2: {db1 is db2}")

    print("\n2.2 Factory Pattern:")
    print(f"  Card: {factory_demo(99.99, 'card')}")
    print(f"  PayPal: {factory_demo(59.99, 'paypal')}")

    print("\n2.3 Strategy Pattern:")
    cart: ShoppingCart = ShoppingCart()
    cart.add_item("Laptop")
    cart.add_item("Mouse")

    cart.set_payment_strategy(CreditCardPayment())
    cart.checkout(999.99)

    cart.set_payment_strategy(PayPalPayment())
    cart.checkout(59.99)
