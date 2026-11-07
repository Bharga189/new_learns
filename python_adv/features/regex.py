# -*- coding: utf-8 -*-
"""
Feature 10: Regular Expressions
re module, pattern matching, groups, lookahead/lookbehind, re.sub, re.split.
"""

import re


def regex_demo() -> None:
    print("\n" + "=" * 70)
    print("10. REGULAR EXPRESSIONS")
    print("=" * 70)

    # Basic pattern matching
    print("\n10.1 Basic Pattern Matching:")
    text = "The email is user@example.com and contact@company.org"

    # Search for email pattern
    email_pattern = r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'
    emails = re.findall(email_pattern, text)
    print(f"  Emails found: {emails}")

    # Replace email
    masked = re.sub(email_pattern, '[email]', text)
    print(f"  Masked text: {masked}")

    # Phone number extraction
    phone_text = "Call me at 555-1234 or 555-5678"
    phones = re.findall(r'\d{3}-\d{4}', phone_text)
    print(f"  Phone numbers: {phones}")

    # Word boundaries
    print("\n10.2 Word Boundaries:")
    sentence = "The cat and the dog"
    # \b matches word boundary
    words = re.findall(r'\b\w{3}\b', sentence)
    print(f"  Three-letter words: {words}")

    # Character classes
    print("\n10.3 Character Classes:")
    text = "Numbers: 123, Letters: abc, Symbols: !@#"
    digits = re.findall(r'\d+', text)
    print(f"  Digits: {digits}")

    letters = re.findall(r'[a-zA-Z]+', text)
    print(f"  Letters: {letters}")

    # Grouping
    print("\n10.4 Groups and Capturing:")
    pattern = r'(\d{3})-(\d{4})'
    match = re.search(pattern, "Call 555-1234")
    if match:
        print(f"  Full match: {match.group(0)}")
        print(f"  Area code: {match.group(1)}")
        print(f"  Number: {match.group(2)}")

    # Named groups
    print("\n10.5 Named Groups:")
    pattern = r'(?P<area>\d{3})-(?P<number>\d{4})'
    match = re.search(pattern, "Call 555-1234")
    if match:
        print(f"  Named group: {match.groupdict()}")

    # Lookahead/lookbehind
    print("\n10.6 Lookahead and Lookbehind:")
    text = "I have $5 and $10"
    # Match dollar amounts followed by space
    amounts = re.findall(r'\$\d+(?=\s)', text)
    print(f"  Amounts with lookahead: {amounts}")

    # Split with pattern
    print("\n10.7 Split:")
    text = "apple,banana;cherry|date"
    fruits = re.split(r'[,;|]', text)
    print(f"  Split by multiple delimiters: {fruits}")


if __name__ == "__main__":
    regex_demo()
