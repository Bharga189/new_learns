# -*- coding: utf-8 -*-
"""
Feature 14b: OOP - Inheritance and Encapsulation
Base classes, inheritance, polymorphism, abstract base classes.
"""

class Animal:
    species_count: int = 0

    def __init__(self, name: str, age: int = 0) -> None:
        self.name: str = name
        self.age: int = age
        Animal.species_count += 1

    def speak(self) -> str:
        return f"{self.name} makes a sound"

    def info(self) -> str:
        return f"{self.name} is {self.age} years old"

    def __str__(self) -> str:
        return f"Animal({self.name}, {self.age})"

    def __repr__(self) -> str:
        return f"Animal(name='{self.name}', age={self.age})"


class Dog(Animal):
    def __init__(self, name: str, age: int = 0, breed: str = "Unknown") -> None:
        super().__init__(name, age)
        self.breed: str = breed

    def speak(self) -> str:
        return f"{self.name} says: Woof!"

    def fetch(self, item: str) -> str:
        return f"{self.name} fetches the {item}!"


class Cat(Animal):
    def speak(self) -> str:        return f"{self.name} says: Meow!"


class BankAccount:
    def __init__(self, owner: str, balance: float = 0.0) -> None:
        self.owner: str = owner
        self.__balance: float = balance

    @property
    def balance(self) -> float:
        return self.__balance

    @balance.setter
    def balance(self, value: float) -> None:
        if value < 0:
            raise ValueError("Balance can't be negative")
        self.__balance = value

    def deposit(self, amount: float) -> float:
        if amount <= 0:
            raise ValueError("Amount must be positive")
        self.__balance += amount
        return self.__balance

    def withdraw(self, amount: float) -> float:
        if amount <= 0:
            raise ValueError("Amount must be positive")
        if amount > self.__balance:
            raise ValueError("Insufficient funds")
        self.__balance -= amount
        return self.__balance

    def __str__(self) -> str:
        return f"BankAccount(owner={self.owner}, balance=${self.__balance:.2f})"


def demo() -> None:
    print("\n" + "---" * 20)
    print("14. BASIC OOP - INHERITANCE AND ENCAPSULATION")
    print("---" * 20)

    print("\n1. Creating Objects:")
    dog: Dog = Dog("Buddy", 3, "Golden Retriever")
    cat: Cat = Cat("Whiskers", 2)

    print(f"   Dog: {dog}")
    print(f"   Cat: {cat}")
    print(f"   Dog repr: {repr(dog)}")

    print("\n2. Method Calls:")
    print(f"   Dog speaks: {dog.speak()}")
    print(f"   Cat speaks: {cat.speak()}")
    print(f"   Dog info: {dog.info()}")
    print(f"   Dog fetch: {dog.fetch('ball')}")

    print("\n3. Inheritance:")
    print(f"   Dog is Animal: {isinstance(dog, Animal)}")
    print(f"   Cat is Animal: {isinstance(cat, Animal)}")
    print(f"   Dog is Dog: {isinstance(dog, Dog)}")
    print(f"   Cat is Dog: {isinstance(cat, Dog)}")
    print(f"   Dog class: {type(dog).__name__}")

    print("\n4. Encapsulation (BankAccount):")
    account: BankAccount = BankAccount("Alice", 1000)
    print(f"   Account: {account}")
    print(f"   Balance: ${account.balance:.2f}")
    account.deposit(500)
    print(f"   After deposit: ${account.balance:.2f}")
    account.withdraw(200)
    print(f"   After withdrawal: ${account.balance:.2f}")
    try:
        print(f"   __balance: {account.__balance}")
    except AttributeError as e:
        print(f"   Can't access private attribute: {e}")

    print("\n" + "=" * 70)
    print("END OF PYTHON BASIC GUIDE")
    print("=" * 70)
