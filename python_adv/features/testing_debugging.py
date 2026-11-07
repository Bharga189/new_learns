# -*- coding: utf-8 -*-
"""
Feature 11: Testing and Debugging
unittest, pytest, mocking (unittest.mock), assert, logging, pdb.
"""

import unittest
from unittest.mock import Mock, patch, MagicMock
import logging


def testing_demo() -> None:
    print("\n" + "=" * 70)
    print("11. TESTING AND DEBUGGING")
    print("=" * 70)

    # Logging
    print("\n11.1 Logging:")
    # Configure logging
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    logger = logging.getLogger("demo")
    logger.debug("This is a debug message")
    logger.info("This is an info message")
    logger.warning("This is a warning message")
    logger.error("This is an error message")
    logger.critical("This is a critical message")

    # Debugging with assert
    print("\n11.2 Debugging with Assert:")
    def divide(a: float, b: float) -> float:
        """Divide two numbers with validation"""
        assert b != 0, "Division by zero is not allowed"
        assert isinstance(a, (int, float)), "a must be a number"
        assert isinstance(b, (int, float)), "b must be a number"
        return a / b

    try:
        result = divide(10, 2)
        print(f"  10 / 2 = {result}")
        divide(10, 0)
    except AssertionError as e:
        print(f"  Assertion caught: {e}")

    # Mocking
    print("\n11.3 Mocking:")
    def get_user_data(user_id: int) -> dict:
        """Get user data from database"""
        return {
            'id': user_id,
            'name': f'User_{user_id}',
            'email': f'user_{user_id}@example.com'
        }

    # Test with mocked database
    with patch('builtins.open', return_value=open('dummy.txt', 'w')) as mock_file:
        mock_file.read.return_value = '{"id": 1, "name": "Test"}'
        print(f"  Mock file read: {mock_file.read.return_value}")

    # Create a mock
    mock_func = Mock(return_value="mocked_result")
    result = mock_func("arg1", "arg2")
    print(f"  Mock function result: {result}")
    print(f"  Mock was called: {mock_func.called}")
    print(f"  Call args: {mock_func.call_args}")

    # Test database call
    mocked_db_func = Mock(return_value={"id": 1, "name": "Test"})
    data = get_user_data(mocked_db_func())
    print(f"  Mock db result: {data}")


class TestDemo(unittest.TestCase):
    """Unittest example for the testing demo"""

    def test_divide(self):
        self.assertEqual(divide(10, 2), 5.0)

    def test_divide_by_zero(self):
        with self.assertRaises(AssertionError):
            divide(10, 0)


def divide(a: float, b: float) -> float:
    """Divide two numbers with validation"""
    assert b != 0, "Division by zero is not allowed"
    assert isinstance(a, (int, float)), "a must be a number"
    assert isinstance(b, (int, float)), "b must be a number"
    return a / b


if __name__ == "__main__":
    testing_demo()
    unittest.main(verbosity=0)
