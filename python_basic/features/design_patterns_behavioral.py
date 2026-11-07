# -*- coding: utf-8 -*-
"""
Feature 22b: Design Patterns - Behavioral
Strategy, Observer, and Decorator patterns.
"""

from functools import wraps
from typing import Callable, List


class Observer:
    def __init__(self, name: str) -> None:
        self.name: str = name

    def update(self, subject: "Subject", message: str) -> None:
        print(f"  {self.name} received: {message}")


class Subject:
    def __init__(self, name: str) -> None:
        self.name: str = name
        self._observers: List = []
        self._state: str = ""

    @property
    def state(self) -> str:
        return self._state

    @state.setter
    def state(self, value: str) -> None:
        self._state = value
        self._notify_observers()

    def attach(self, observer: Observer) -> None:
        self._observers.append(observer)

    def detach(self, observer: Observer) -> None:
        self._observers.remove(observer)

    def _notify_observers(self) -> None:
        for observer in self._observers:
            observer.update(self, f"State changed to: {self._state}")


def make_bold(func: Callable) -> Callable:
    @wraps(func)
    def wrapper(*args, **kwargs) -> str:
        return f"<b>{func(*args, **kwargs)}</b>"
    return wrapper


def make_italic(func: Callable) -> Callable:
    @wraps(func)
    def wrapper(*args, **kwargs) -> str:
        return f"<i>{func(*args, **kwargs)}</i>"
    return wrapper


def demo() -> None:
    print("\n" + "---" * 20)
    print("22b. DESIGN PATTERNS - BEHAVIORAL")
    print("---" * 20)

    print("\n2.3 Observer Pattern:")
    subject: Subject = Subject("NewsService")

    observer1: Observer = Observer("Subscriber 1")
    observer2: Observer = Observer("Subscriber 2")

    subject.attach(observer1)
    subject.attach(observer2)

    subject.state = "Breaking news: Python 4.0 released!"
    subject.state = "Update: New features announced"

    print("\n2.4 Decorator Pattern:")
    @make_bold
    @make_italic
    def get_text() -> str:
        return "Hello!"

    print(f"  Decorated text: {get_text()}")
