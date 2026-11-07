# -*- coding: utf-8 -*-
"""
Feature 20: Testing and Debugging
Logging, assert, mocking (unittest.mock), pytest-style.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("20. TESTING AND DEBUGGING")
    print("---" * 20)

    print("\n2.1 Logging:")
    import logging
    logging.basicConfig(
        level=logging.DEBUG,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    logger: logging.Logger = logging.getLogger("demo")
    logger.debug("This is a debug message")
    logger.info("This is an info message")
    logger.warning("This is a warning message")
    logger.error("This is an error message")
    logger.critical("This is a critical message")

    print("\n2.2 Debugging with Assert:")

    def divide(a: float, b: float) -> float:
        if b == 0:
            raise ZeroDivisionError("Division by zero is not allowed")
        if not isinstance(a, (int, float)):
            raise TypeError("a must be a number")
        if not isinstance(b, (int, float)):
            raise TypeError("b must be a number")
        return a / b

    try:
        result: float = divide(10, 2)
        print(f"  10 / 2 = {result}")
        divide(10, 0)
    except ZeroDivisionError as e:
        print(f"  Exception caught: {e}")

    print("\n2.3 Mocking:")
    from unittest.mock import Mock, patch

    def get_user_data(user_id: int) -> dict:
        return {
            "id": user_id,
            "name": f"User_{user_id}",
            "email": f"user_{user_id}@example.com"
        }

    mock_func: Mock = Mock(return_value="mocked_result")
    result: str = mock_func("arg1", "arg2")
    print(f"  Mock function result: {result}")
    print(f"  Mock was called: {mock_func.called}")
    print(f"  Call args: {mock_func.call_args}")

    mocked_db_func: Mock = Mock(return_value={"id": 1, "name": "Test"})
    data: dict = get_user_data(mocked_db_func())
    print(f"  Mock db result: {data}")

    print("\n2.4 Assert with pytest style:")
    def test_function() -> None:
        assert divide(10, 2) == 5.0
        try:
            divide(10, 0)
            print("  ERROR: Expected ZeroDivisionError was not raised")
            return
        except ZeroDivisionError as e:
            print(f"  Assertion caught: {e}")

    test_function()
    print("  Test function passed (without exceptions)")
