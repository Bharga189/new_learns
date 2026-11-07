# -*- coding: utf-8 -*-
"""
Feature 5: Advanced OOP
Multiple inheritance, MRO, super(), ABC, class/static methods, properties,
descriptors, metaclasses, dataclasses, mixins.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass


class Animal(ABC):
    """Abstract base class for animals"""

    def __init__(self, name):
        self.name = name

    @abstractmethod
    def speak(self):
        """All animals must implement speak"""
        pass

    def sleep(self):
        """Common method"""
        return f"{self.name} is sleeping"


class Dog(Animal):
    """Dog class - implements abstract method"""

    def speak(self):
        return f"{self.name} says: Woof!"


class Cat(Animal):
    """Cat class - implements abstract method"""

    def speak(self):
        return f"{self.name} says: Meow!"


class Bird(Animal):
    """Bird class - implements abstract method"""

    def speak(self):
        return f"{self.name} says: Chirp!"


class DatabaseConnection:
    """Base connection class"""

    def connect(self):
        return "Connected to database"

    def disconnect(self):
        return "Disconnected from database"


class Loggable:
    """Mixin class for logging"""

    def log(self, message):
        print(f"  [LOG] {message}")
        return message


class Person:
    """Person class demonstrating encapsulation"""

    def __init__(self, name, age):
        self._name = name  # Protected attribute
        self.__age = age   # Private attribute (name mangling)

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, value):
        if not value:
            raise ValueError("Name can't be empty")
        self._name = value

    @property
    def age(self):
        return self.__age

    @age.setter
    def age(self, value):
        if value < 0:
            raise ValueError("Age can't be negative")
        self.__age = value

    def __str__(self):
        return f"Person(name={self._name}, age={self.__age})"


def advanced_oop_demo() -> None:
    print("\n" + "=" * 70)
    print("5. ADVANCED OOP")
    print("=" * 70)

    # Abstract base classes
    print("\n5.1 Abstract Base Classes:")
    print("  Creating animals:")
    try:
        animal = Animal("Generic")
    except TypeError as e:
        print(f"  Can't instantiate abstract class: {e}")

    dog = Dog("Buddy")
    cat = Cat("Whiskers")
    bird = Bird("Tweety")

    print(f"  Dog: {dog.speak()}")
    print(f"  Cat: {cat.speak()}")
    print(f"  Bird: {bird.speak()}")
    print(f"  Dog sleeps: {dog.sleep()}")

    # Multiple inheritance and MRO
    print("\n5.2 Multiple Inheritance and MRO:")
    class Logger:
        def log(self):
            return "Logging"

    class Database:
        def connect(self):
            return "Database connection"

    class SmartApp(Database, Logger):
        pass

    app = SmartApp()
    print(f"  MRO: {SmartApp.__mro__}")
    print(f"  SmartApp connects: {app.connect()}")
    print(f"  SmartApp logs: {app.log()}")

    # Class and static methods
    print("\n5.3 Class and Static Methods:")
    class Circle:
        pi = 3.14159

        def __init__(self, radius):
            self.radius = radius

        @classmethod
        def from_diameter(cls, diameter):
            return cls(diameter / 2)

        @staticmethod
        def is_valid_radius(radius):
            return radius > 0

        def area(self):
            return Circle.pi * self.radius ** 2

    c1 = Circle(5)
    c2 = Circle.from_diameter(10)
    print(f"  c1 area: {c1.area():.2f}")
    print(f"  c2 radius: {c2.radius}")
    print(f"  is_valid_radius(5): {Circle.is_valid_radius(5)}")

    # Properties
    print("\n5.4 Properties (Encapsulation):")
    print("  Creating Person:")
    person = Person("Alice", 30)
    print(f"  Person: {person}")
    print(f"  Name: {person.name}")
    print(f"  Age: {person.age}")

    print("  Setting name...")
    person.name = "Bob"
    print(f"  New name: {person.name}")

    print("  Setting age...")
    person.age = 25
    print(f"  New age: {person.age}")

    try:
        print("  Setting invalid age...")
        person.age = -5
    except ValueError as e:
        print(f"  Error: {e}")

    try:
        print("  Setting empty name...")
        person.name = ""
    except ValueError as e:
        print(f"  Error: {e}")

    # Dataclasses
    print("\n5.5 Dataclasses:")
    @dataclass
    class Point:
        x: float
        y: float
        label: str = "origin"

    p1 = Point(1.0, 2.0, "A")
    p2 = Point(1.0, 2.0)

    print(f"  p1: {p1}")
    print(f"  p2: {p2}")
    print(f"  p1 == p2: {p1 == p2}")


if __name__ == "__main__":
    advanced_oop_demo()
