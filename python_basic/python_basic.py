#!/usr/bin/env python3
"""
Python Basic - Comprehensive Guide to Core Python Features
==========================================================

This module covers the fundamental building blocks of Python programming.
Each section includes examples and detailed explanations.

Topics Covered:
1. Variables and Data Types
2. Operators
3. Control Flow (if/else, loops)
4. Functions
5. Strings
6. Lists
7. Tuples
8. Dictionaries
9. Sets
10. Basic File I/O
11. Error Handling
12. Modules and Imports
13. List Comprehensions
14. Basic OOP
"""

# =============================================================================
# 1. VARIABLES AND DATA TYPES
# =============================================================================

"""
Python is dynamically typed - you don't need to declare variable types.
Variables are created when you assign a value to them.

Data Types:
- int: Integer numbers (e.g., 5, -10, 0)
- float: Decimal numbers (e.g., 3.14, -0.01)
- str: Text strings (e.g., "hello", 'world')
- bool: Boolean values (True, False)
- None: Represents absence of value
- list: Ordered mutable collection
- tuple: Ordered immutable collection
- dict: Key-value pairs
- set: Unordered unique elements

Type Conversion:
- int(): Convert to integer
- float(): Convert to float
- str(): Convert to string
- bool(): Convert to boolean
- list(), tuple(), dict(), set(): Convert to respective types
"""

def variables_and_types_demo():
    """Demonstrate variables and data types"""
    
    # Basic variable assignment
    integer_var = 42
    float_var = 3.14159
    string_var = "Hello, Python!"
    boolean_var = True
    none_var = None
    
    print(f"Integer: {integer_var} (type: {type(integer_var).__name__})")
    print(f"Float: {float_var} (type: {type(float_var).__name__})")
    print(f"String: {string_var} (type: {type(string_var).__name__})")
    print(f"Boolean: {boolean_var} (type: {type(boolean_var).__name__})")
    print(f"None: {none_var} (type: {type(none_var).__name__})")
    
    # Type conversion
    print("\n--- Type Conversion ---")
    print(f"int(3.9) = {int(3.9)}")  # Truncates to 3
    print(f"float(5) = {float(5)}")  # Becomes 5.0
    print(f"str(100) = {str(100)}")  # Becomes "100"
    print(f"bool(0) = {bool(0)}")    # False (0 is falsy)
    print(f"bool(1) = {bool(1)}")    # True (non-zero is truthy)
    print(f"bool('') = {bool('')}")  # False (empty string is falsy)
    print(f"bool('hello') = {bool('hello')}")  # True (non-empty is truthy)


# =============================================================================
# 2. OPERATORS
# =============================================================================

"""
Operators in Python:

Arithmetic Operators:
- + : Addition
- - : Subtraction
- * : Multiplication
- / : Division (returns float)
- // : Floor division (returns integer)
- % : Modulus (remainder)
- ** : Exponentiation

Comparison Operators:
- == : Equal to
- != : Not equal to
- > : Greater than
- < : Less than
- >= : Greater than or equal
- <= : Less than or equal

Logical Operators:
- and : Returns True if both are True
- or : Returns True if at least one is True
- not : Negates the boolean value

Assignment Operators:
- = : Simple assignment
- += : Add and assign
- -= : Subtract and assign
- *= : Multiply and assign
- /= : Divide and assign
- %= : Modulus and assign
- **= : Exponent and assign
- //= : Floor divide and assign

Identity Operators:
- is : True if both variables point to same object
- is not : True if they don't point to same object

Membership Operators:
- in : True if value is in sequence
- not in : True if value is not in sequence
"""

def operators_demo():
    """Demonstrate various operators"""
    
    print("\n--- Arithmetic Operators ---")
    a, b = 10, 3
    print(f"{a} + {b} = {a + b}")  # 13
    print(f"{a} - {b} = {a - b}")  # 7
    print(f"{a} * {b} = {a * b}")  # 30
    print(f"{a} / {b} = {a / b}")  # 3.333...
    print(f"{a} // {b} = {a // b}")  # 3 (floor division)
    print(f"{a} % {b} = {a % b}")  # 1 (remainder)
    print(f"{a} ** {b} = {a ** b}")  # 1000 (10^3)
    
    print("\n--- Comparison Operators ---")
    x, y = 5, 10
    print(f"{x} == {y}: {x == y}")  # False
    print(f"{x} != {y}: {x != y}")  # True
    print(f"{x} > {y}: {x > y}")    # False
    print(f"{x} < {y}: {x < y}")    # True
    print(f"{x} >= {y}: {x >= y}")  # False
    print(f"{x} <= {y}: {x <= y}")  # True
    
    print("\n--- Logical Operators ---")
    p, q = True, False
    print(f"{p} and {q}: {p and q}")  # False
    print(f"{p} or {q}: {p or q}")    # True
    print(f"not {p}: {not p}")        # False
    print(f"not {q}: {not q}")        # True
    
    print("\n--- Identity Operators ---")
    list1 = [1, 2, 3]
    list2 = list1
    list3 = [1, 2, 3]
    print(f"list1 is list2: {list1 is list2}")  # True (same object)
    print(f"list1 is list3: {list1 is list3}")  # False (different objects)
    print(f"list1 == list3: {list1 == list3}")  # True (same values)
    
    print("\n--- Membership Operators ---")
    numbers = [1, 2, 3, 4, 5]
    print(f"3 in numbers: {3 in numbers}")     # True
    print(f"10 not in numbers: {10 not in numbers}")  # True


# =============================================================================
# 3. CONTROL FLOW
# =============================================================================

"""
Control Flow in Python:

Conditional Statements:
- if: Execute block if condition is True
- elif: Execute block if previous conditions were False and this is True
- else: Execute block if all previous conditions were False

Loops:
- for: Iterate over a sequence (list, tuple, string, etc.)
- while: Execute block as long as condition is True

Loop Control:
- break: Exit the loop immediately
- continue: Skip to next iteration
- pass: Do nothing (placeholder)

Best Practices:
- Use enumerate() for index and value in for loops
- Use range() for numeric iteration
- Use zip() to iterate over multiple sequences
"""

def control_flow_demo():
    """Demonstrate control flow statements"""
    
    print("\n--- If/Elif/Else ---")
    score = 85
    
    if score >= 90:
        grade = "A"
    elif score >= 80:
        grade = "B"
    elif score >= 70:
        grade = "C"
    elif score >= 60:
        grade = "D"
    else:
        grade = "F"
    
    print(f"Score {score} = Grade {grade}")
    
    print("\n--- For Loop ---")
    # Basic for loop
    fruits = ["apple", "banana", "cherry"]
    for fruit in fruits:
        print(f"  I like {fruit}")
    
    # For loop with range
    print("\nCounting 1 to 5:")
    for i in range(1, 6):
        print(f"  {i}", end=" ")
    print()
    
    # For loop with enumerate (gets index and value)
    print("\nWith enumerate:")
    for index, fruit in enumerate(fruits):
        print(f"  [{index}] {fruit}")
    
    # For loop with zip (iterate multiple sequences)
    print("\nWith zip:")
    colors = ["red", "yellow", "red"]
    for fruit, color in zip(fruits, colors):
        print(f"  {fruit} is {color}")
    
    print("\n--- While Loop ---")
    count = 0
    while count < 5:
        print(f"  Count: {count}")
        count += 1
    
    print("\n--- Break and Continue ---")
    for i in range(10):
        if i == 3:
            print(f"  Skipping {i}")
            continue  # Skip this iteration
        if i == 7:
            print(f"  Breaking at {i}")
            break     # Exit loop
        print(f"  Processing {i}")


# =============================================================================
# 4. FUNCTIONS
# =============================================================================

"""
Functions in Python:

- Defined using 'def' keyword
- Can have parameters with default values
- Can return multiple values (as tuple)
- Support *args (variable positional arguments)
- Support **kwargs (variable keyword arguments)
- Are first-class objects (can be passed around)
- Can have type hints (Python 3.5+)
- Can have docstrings (documentation)
- Support lambda (anonymous functions)

Scope:
- Local: Inside function
- Enclosing: In enclosing function
- Global: At module level
- Built-in: Python built-in functions

Best Practices:
- Use descriptive names
- Keep functions small and focused
- Use docstrings to document functions
- Use type hints for clarity
"""

def no_params():
    """Function with no parameters"""
    return "Hello from no_params!"

def with_params(name, age):
    """Function with parameters"""
    return f"{name} is {age} years old"

def default_params(name="Guest", greeting="Hello"):
    """Function with default parameter values"""
    return f"{greeting}, {name}!"

def variable_args(*args):
    """Function with variable positional arguments"""
    print(f"  args: {args} (type: {type(args).__name__})")
    return sum(args)

def variable_kwargs(**kwargs):
    """Function with variable keyword arguments"""
    print(f"  kwargs: {kwargs} (type: {type(kwargs).__name__})")
    return kwargs

def mixed_params(a, b, *args, c=10, **kwargs):
    """Function mixing different parameter types"""
    print(f"  a={a}, b={b}, args={args}, c={c}, kwargs={kwargs}")
    return a + b + sum(args) + c

def return_multiple():
    """Function returning multiple values"""
    name = "Alice"
    age = 30
    city = "New York"
    return name, age, city  # Returns as tuple

def lambda_demo():
    """Demonstrate lambda (anonymous) functions"""
    # Lambda is a small anonymous function
    # Syntax: lambda arguments: expression
    square = lambda x: x ** 2
    add = lambda x, y: x + y
    
    print(f"  square(5) = {square(5)}")
    print(f"  add(3, 4) = {add(3, 4)}")
    
    # Lambda often used with map, filter, reduce
    numbers = [1, 2, 3, 4, 5]
    squared = list(map(lambda x: x ** 2, numbers))
    evens = list(filter(lambda x: x % 2 == 0, numbers))
    
    print(f"  Squared: {squared}")
    print(f"  Evens: {evens}")

def functions_demo():
    """Run all function demonstrations"""
    print("\n--- Functions ---")
    
    print("\nNo params:")
    print(f"  {no_params()}")
    
    print("\nWith params:")
    print(f"  {with_params('Bob', 25)}")
    
    print("\nDefault params:")
    print(f"  {default_params()}")
    print(f"  {default_params('Alice')}")
    print(f"  {default_params('Alice', 'Hi')}")
    print(f"  {default_params(greeting='Hey')}")  # Keyword argument
    
    print("\nVariable args:")
    result = variable_args(1, 2, 3, 4, 5)
    print(f"  Sum: {result}")
    
    print("\nVariable kwargs:")
    result = variable_kwargs(name="Charlie", age=35)
    print(f"  Result: {result}")
    
    print("\nMixed params:")
    result = mixed_params(1, 2, 3, 4, 5, c=100, x=1, y=2)
    print(f"  Result: {result}")
    
    print("\nReturn multiple values:")
    name, age, city = return_multiple()
    print(f"  name={name}, age={age}, city={city}")
    
    lambda_demo()


# =============================================================================
# 5. STRINGS
# =============================================================================

"""
Strings in Python:

- Immutable sequence of characters
- Can use single ('), double ("), or triple (''' or \"\"\") quotes
- Support indexing and slicing
- Rich set of string methods
- Support f-strings (formatted string literals, Python 3.6+)
- Support string formatting with .format() or % operator

String Methods:
- Upper/Lower case: upper(), lower(), capitalize(), title(), swapcase()
- Stripping: strip(), lstrip(), rstrip()
- Searching: find(), rfind(), index(), rindex(), count()
- Replacing: replace()
- Splitting/Joining: split(), splitlines(), join()
- Checking: startswith(), endswith(), isalpha(),isdigit(), etc.
- Formatting: center(), ljust(), rjust(), zfill()

String Formatting:
- f-strings: f"Hello {name}"
- .format(): "Hello {}".format(name)
- % operator: "Hello %s" % name
"""

def strings_demo():
    """Demonstrate string operations"""
    print("\n--- Strings ---")
    
    # Different quote styles
    s1 = 'Single quotes'
    s2 = "Double quotes"
    s3 = '''Triple single quotes
    for multi-line'''
    s4 = """Triple double quotes
    for multi-line"""
    
    print(f"s1: {s1}")
    print(f"s2: {s2}")
    print(f"Multi-line:\n{s3}")
    
    # f-strings (formatted string literals)
    name = "Alice"
    age = 30
    print(f"\nf-string example: My name is {name} and I'm {age}")
    print(f"Expression in f-string: 2 + 3 = {2 + 3}")
    print(f"Formatted number: {3.14159:.2f}")  # 3.14
    print(f"Formatted number: {1000000:,}")    # 1,000,000
    
    # String methods
    text = "  Hello, World!  "
    print(f"\nOriginal: '{text}'")
    print(f"strip(): '{text.strip()}'")
    print(f"lower(): '{text.lower()}'")
    print(f"upper(): '{text.upper()}'")
    print(f"capitalize(): '{text.capitalize()}'")
    print(f"title(): '{text.title()}'")
    print(f"replace('World', 'Python'): '{text.replace('World', 'Python')}'")
    
    # Splitting and joining
    csv_data = "apple,banana,cherry"
    fruits_list = csv_data.split(",")
    print(f"\nSplit: {fruits_list}")
    joined = " | ".join(fruits_list)
    print(f"Join: {joined}")
    
    # String searching
    sentence = "The quick brown fox jumps over the lazy dog"
    print(f"\nFind 'fox': position {sentence.find('fox')}")
    print(f"Count 'the' (case insensitive): {sentence.lower().count('the')}")
    print(f"Starts with 'The': {sentence.startswith('The')}")
    print(f"Ends with 'dog': {sentence.endswith('dog')}")
    
    # String slicing
    word = "Python"
    print(f"\nWord: {word}")
    print(f"word[0]: {word[0]}")      # First character
    print(f"word[-1]: {word[-1]}")    # Last character
    print(f"word[0:3]: {word[0:3]}")  # First 3 characters
    print(f"word[3:]: {word[3:]}")    # From index 3 to end
    print(f"word[:3]: {word[:3]}")    # From start to index 3
    print(f"word[::-1]: {word[::-1]}")  # Reverse string


# =============================================================================
# 6. LISTS
# =============================================================================

"""
Lists in Python:

- Ordered, mutable collection
- Can contain mixed types
- Support indexing, slicing, and various methods
- Dynamic size (can grow/shrink)

List Methods:
- append(): Add element to end
- extend(): Add multiple elements
- insert(): Insert at position
- remove(): Remove first occurrence
- pop(): Remove and return element
- clear(): Remove all elements
- index(): Find index of element
- count(): Count occurrences
- sort(): Sort in place
- reverse(): Reverse in place
- copy(): Create shallow copy

List Operations:
- + : Concatenate lists
- * : Repeat list
- in : Membership test
- len(): Get length

List Comprehensions:
- Concise way to create lists
- Syntax: [expression for item in iterable if condition]
"""

def lists_demo():
    """Demonstrate list operations"""
    print("\n--- Lists ---")
    
    # Creating lists
    empty_list = []
    list_with_values = [1, 2, 3, 4, 5]
    mixed_list = [1, "hello", 3.14, True, [1, 2]]
    
    print(f"Empty list: {empty_list}")
    print(f"List with values: {list_with_values}")
    print(f"Mixed list: {mixed_list}")
    
    # List indexing and slicing
    numbers = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
    print(f"\nList: {numbers}")
    print(f"numbers[0]: {numbers[0]}")
    print(f"numbers[-1]: {numbers[-1]}")
    print(f"numbers[2:5]: {numbers[2:5]}")
    print(f"numbers[::2]: {numbers[::2]}")  # Every other element
    print(f"numbers[::-1]: {numbers[::-1]}")  # Reversed
    
    # Modifying lists
    print("\n--- Modifying Lists ---")
    fruits = ["apple", "banana", "cherry"]
    print(f"Original: {fruits}")
    
    fruits.append("date")
    print(f"After append: {fruits}")
    
    fruits.insert(1, "apricot")
    print(f"After insert at 1: {fruits}")
    
    fruits.extend(["elderberry", "fig"])
    print(f"After extend: {fruits}")
    
    removed = fruits.pop()
    print(f"After pop: {fruits}, removed: {removed}")
    
    fruits.remove("banana")
    print(f"After remove 'banana': {fruits}")
    
    # List methods
    print("\n--- List Methods ---")
    nums = [3, 1, 4, 1, 5, 9, 2, 6]
    print(f"List: {nums}")
    print(f"index(5): {nums.index(5)}")
    print(f"count(1): {nums.count(1)}")
    nums.sort()
    print(f"After sort: {nums}")
    nums.reverse()
    print(f"After reverse: {nums}")
    nums_copy = nums.copy()
    print(f"Copy: {nums_copy}")
    
    # List operations
    print("\n--- List Operations ---")
    list1 = [1, 2, 3]
    list2 = [4, 5, 6]
    print(f"list1 + list2 = {list1 + list2}")
    print(f"list1 * 3 = {list1 * 3}")
    print(f"2 in list1: {2 in list1}")
    print(f"len(list1): {len(list1)}")
    
    # List comprehensions
    print("\n--- List Comprehensions ---")
    numbers = range(10)
    
    # Basic comprehension
    squares = [x ** 2 for x in numbers]
    print(f"Squares: {squares}")
    
    # With condition
    evens = [x for x in numbers if x % 2 == 0]
    print(f"Evens: {evens}")
    
    # Nested comprehension
    matrix = [[i * j for j in range(3)] for i in range(3)]
    print(f"Matrix: {matrix}")
    
    # With if-else
    labels = ["even" if x % 2 == 0 else "odd" for x in numbers]
    print(f"Labels: {labels}")


# =============================================================================
# 7. TUPLES
# =============================================================================

"""
Tuples in Python:

- Ordered, immutable collection
- Can contain mixed types
- Faster than lists for iteration
- Support unpacking
- Can be used as dictionary keys (if all elements are hashable)
- Single element tuple needs trailing comma: (1,)

Tuple Unpacking:
- a, b, c = tuple
- Useful for returning multiple values from functions

When to Use Tuples vs Lists:
- Use tuples for fixed data that shouldn't change
- Tuples as dictionary keys
- Tuples as function return values
- Lists for mutable collections
"""

def tuples_demo():
    """Demonstrate tuple operations"""
    print("\n--- Tuples ---")
    
    # Creating tuples
    empty_tuple = ()
    single_tuple = (1,)  # Note the comma!
    multi_tuple = (1, 2, 3, "hello", 3.14)
    mixed_tuple = (1, "hello", [1, 2], {"key": "value"})
    
    print(f"Empty tuple: {empty_tuple}")
    print(f"Single tuple: {single_tuple}")
    print(f"Multi tuple: {multi_tuple}")
    print(f"Mixed tuple: {mixed_tuple}")
    
    # Tuple operations
    print(f"\nLength: {len(multi_tuple)}")
    print(f"First element: {multi_tuple[0]}")
    print(f"Last element: {multi_tuple[-1]}")
    print(f"Slicing [1:3]: {multi_tuple[1:3]}")
    print(f"Count of 1: {multi_tuple.count(1)}")
    print(f"Index of 'hello': {multi_tuple.index('hello')}")
    
    # Immutability
    print("\n--- Immutability ---")
    try:
        multi_tuple[0] = 100
    except TypeError as e:
        print(f"Can't modify tuple: {e}")
    
    # Tuple unpacking
    print("\n--- Tuple Unpacking ---")
    coordinates = (10, 20, 30)
    x, y, z = coordinates
    print(f"Unpacked: x={x}, y={y}, z={z}")
    
    # Swapping using tuple unpacking
    a, b = 5, 10
    a, b = b, a  # Swap
    print(f"After swap: a={a}, b={b}")
    
    # Nested tuple unpacking
    person = ("Alice", 30, ("New York", "USA"))
    name, age, (city, country) = person
    print(f"Name: {name}, Age: {age}, City: {city}, Country: {country}")
    
    # When to use tuples
    print("\n--- Tuple Use Cases ---")
    # As dictionary keys
    locations = {
        (40.7128, -74.0060): "New York",
        (34.0522, -118.2437): "Los Angeles",
        (51.5074, -0.1278): "London"
    }
    print(f"Location lookup: {locations[(40.7128, -74.0060)]}")
    
    # Function returns
    def get_stats(numbers):
        return min(numbers), max(numbers), sum(numbers) / len(numbers)
    
    result = get_stats([1, 2, 3, 4, 5])
    print(f"Stats (min, max, avg): {result}")


# =============================================================================
# 8. DICTIONARIES
# =============================================================================

"""
Dictionaries in Python:

- Unordered (Python 3.7+: insertion-ordered) collection of key-value pairs
- Keys must be hashable (immutable types)
- Values can be any type
- Fast lookups by key (O(1) average)

Dictionary Methods:
- keys(): Get all keys
- values(): Get all values
- items(): Get key-value pairs
- get(): Get value with default
- update(): Merge dictionaries
- pop(): Remove and return value
- popitem(): Remove last item
- clear(): Remove all items
- copy(): Create shallow copy
- setdefault(): Get or set default
- fromkeys(): Create from keys

Dictionary Comprehensions:
- {key_expr: value_expr for item in iterable}

Common Patterns:
- Counting frequencies
- Grouping data
- Caching/memoization
"""

def dictionaries_demo():
    """Demonstrate dictionary operations"""
    print("\n--- Dictionaries ---")
    
    # Creating dictionaries
    empty_dict = {}
    dict_literal = {"name": "Alice", "age": 30, "city": "New York"}
    dict_constructor = dict(name="Bob", age=25, city="Boston")
    
    print(f"Empty dict: {empty_dict}")
    print(f"Literal: {dict_literal}")
    print(f"Constructor: {dict_constructor}")
    
    # Accessing values
    print(f"\ndict['name']: {dict_literal['name']}")
    print(f"get('age'): {dict_literal.get('age')}")
    print(f"get('country', 'Unknown'): {dict_literal.get('country', 'Unknown')}")
    
    # Modifying dictionaries
    print("\n--- Modifying Dictionaries ---")
    person = {"name": "Alice", "age": 30}
    print(f"Original: {person}")
    
    person["age"] = 31  # Update
    print(f"After update: {person}")
    
    person["city"] = "New York"  # Add new
    print(f"After add: {person}")
    
    removed = person.pop("city")
    print(f"After pop 'city': {person}, removed: {removed}")
    
    # Dictionary methods
    print("\n--- Dictionary Methods ---")
    data = {"a": 1, "b": 2, "c": 3}
    print(f"Keys: {list(data.keys())}")
    print(f"Values: {list(data.values())}")
    print(f"Items: {list(data.items())}")
    
    # Iterating over dictionary
    print("\nIterating:")
    for key in data:
        print(f"  {key}: {data[key]}")
    
    for key, value in data.items():
        print(f"  {key} = {value}")
    
    # Dictionary operations
    print("\n--- Dictionary Operations ---")
    dict1 = {"a": 1, "b": 2}
    dict2 = {"c": 3, "d": 4}
    merged = {**dict1, **dict2}  # Merge with unpacking (Python 3.5+)
    print(f"Merged: {merged}")
    
    # Check existence
    print(f"'a' in dict1: {'a' in dict1}")
    print(f"1 in dict1.values(): {1 in dict1.values()}")
    
    # Dictionary comprehensions
    print("\n--- Dictionary Comprehensions ---")
    numbers = range(5)
    squares_dict = {x: x ** 2 for x in numbers}
    print(f"Squares dict: {squares_dict}")
    
    # Filter dictionary
    even_squares = {k: v for k, v in squares_dict.items() if v % 2 == 0}
    print(f"Even squares: {even_squares}")
    
    # Common patterns
    print("\n--- Common Patterns ---")
    
    # Counting frequencies
    words = ["apple", "banana", "apple", "cherry", "banana", "apple"]
    word_count = {}
    for word in words:
        word_count[word] = word_count.get(word, 0) + 1
    print(f"Word count: {word_count}")
    
    # Grouping
    people = [
        {"name": "Alice", "age": 30},
        {"name": "Bob", "age": 25},
        {"name": "Charlie", "age": 30}
    ]
    by_age = {}
    for person in people:
        age = person["age"]
        by_age.setdefault(age, []).append(person["name"])
    print(f"Grouped by age: {by_age}")


# =============================================================================
# 9. SETS
# =============================================================================

"""
Sets in Python:

- Unordered collection of unique elements
- Elements must be hashable
- Support mathematical set operations
- Faster membership testing than lists (O(1) average)

Set Operations:
- union (|): Combine sets
- intersection (&): Common elements
- difference (-): Elements in first but not second
- symmetric_difference (^): Elements in either but not both
- issubset (<=): Check subset
- issuperset (>=): Check superset
- isdisjoint: Check no common elements

Set Methods:
- add(): Add single element
- update(): Add multiple elements
- remove(): Remove element (raises error if not found)
- discard(): Remove element (no error if not found)
- pop(): Remove random element
- clear(): Remove all elements

Set Comprehensions:
- {expression for item in iterable}

Use Cases:
- Removing duplicates
- Membership testing
- Mathematical set operations
- Finding unique elements
"""

def sets_demo():
    """Demonstrate set operations"""
    print("\n--- Sets ---")
    
    # Creating sets
    empty_set = set()  # Note: {} creates empty dict!
    set_literal = {1, 2, 3, 4, 5}
    set_from_list = set([1, 2, 2, 3, 3, 4])  # Duplicates removed
    
    print(f"Empty set: {empty_set}")
    print(f"Set literal: {set_literal}")
    print(f"Set from list: {set_from_list}")
    
    # Set operations
    print("\n--- Set Operations ---")
    set1 = {1, 2, 3, 4, 5}
    set2 = {4, 5, 6, 7, 8}
    
    print(f"set1: {set1}")
    print(f"set2: {set2}")
    print(f"Union (set1 | set2): {set1 | set2}")
    print(f"Intersection (set1 & set2): {set1 & set2}")
    print(f"Difference (set1 - set2): {set1 - set2}")
    print(f"Symmetric diff (set1 ^ set2): {set1 ^ set2}")
    
    # Set methods
    print("\n--- Set Methods ---")
    s = {1, 2, 3}
    print(f"Original: {s}")
    
    s.add(4)
    print(f"After add(4): {s}")
    
    s.update([5, 6, 7])
    print(f"After update([5,6,7]): {s}")
    
    s.discard(3)
    print(f"After discard(3): {s}")
    
    s.discard(100)  # No error if not found
    print(f"After discard(100): {s}")
    
    # Set comprehensions
    print("\n--- Set Comprehensions ---")
    numbers = [1, 2, 2, 3, 3, 3, 4, 4, 4, 4]
    unique_squares = {x ** 2 for x in numbers}
    print(f"Unique squares: {unique_squares}")
    
    # Removing duplicates
    print("\n--- Removing Duplicates ---")
    duplicates = [1, 2, 2, 3, 3, 3, 4, 5, 5]
    unique = list(set(duplicates))
    print(f"Original: {duplicates}")
    print(f"Unique: {unique}")


# =============================================================================
# 10. BASIC FILE I/O
# =============================================================================

"""
File I/O in Python:

- Use 'open()' function to open files
- Always use 'with' statement (context manager) for automatic closing
- Common modes:
  - 'r': Read (default)
  - 'w': Write (overwrites)
  - 'a': Append
  - 'r+': Read and write
  - 'b': Binary mode (add to mode, e.g., 'rb', 'wb')

Reading:
- read(): Read entire file
- readline(): Read one line
- readlines(): Read all lines into list
- iterate over file object for line-by-line

Writing:
- write(): Write string
- writelines(): Write list of strings

Path Operations (pathlib or os.path):
- Join paths
- Check existence
- Get file info
"""

import os
from pathlib import Path

def file_io_demo():
    """Demonstrate file I/O operations"""
    print("\n--- File I/O ---")
    
    # Using pathlib for path operations
    current_dir = Path.cwd()
    test_file = current_dir / "test_file.txt"
    
    # Writing to a file
    print(f"Writing to: {test_file}")
    with open(test_file, 'w', encoding='utf-8') as f:
        f.write("Line 1: Hello, World!\n")
        f.write("Line 2: Python is awesome\n")
        f.write("Line 3: File I/O is easy\n")
    
    # Reading entire file
    print("\nReading entire file:")
    with open(test_file, 'r', encoding='utf-8') as f:
        content = f.read()
        print(content)
    
    # Reading line by line
    print("Reading line by line:")
    with open(test_file, 'r', encoding='utf-8') as f:
        for i, line in enumerate(f, 1):
            print(f"  Line {i}: {line.strip()}")
    
    # Reading all lines
    print("\nReading all lines as list:")
    with open(test_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        print(f"Lines: {lines}")
    
    # Appending to file
    print("\nAppending to file:")
    with open(test_file, 'a', encoding='utf-8') as f:
        f.write("Line 4: Appended content\n")
    
    # Reading after append
    with open(test_file, 'r', encoding='utf-8') as f:
        print(f.read())
    
    # File operations with pathlib
    print("\n--- Path Operations ---")
    print(f"Exists: {test_file.exists()}")
    print(f"Is file: {test_file.is_file()}")
    print(f"Name: {test_file.name}")
    print(f"Stem: {test_file.stem}")  # Without extension
    print(f"Suffix: {test_file.suffix}")  # Extension
    print(f"Parent: {test_file.parent}")
    
    # Clean up
    test_file.unlink()
    print(f"\nDeleted: {test_file.exists()}")


# =============================================================================
# 11. ERROR HANDLING
# =============================================================================

"""
Error Handling in Python:

- try: Block where exception might occur
- except: Handle specific exceptions
- else: Execute if no exception (optional)
- finally: Always execute (optional)

Exception Hierarchy:
- BaseException
  - SystemExit
  - KeyboardInterrupt
  - Exception
    - ArithmeticError (ZeroDivisionError, etc.)
    - LookupError (IndexError, KeyError, etc.)
    - TypeError
    - ValueError
    - ... and many more

Creating Custom Exceptions:
- Inherit from Exception class
- Add custom attributes and methods

Best Practices:
- Catch specific exceptions, not bare except
- Use finally for cleanup
- Don't suppress exceptions without reason
- Use raise to re-raise or raise new exceptions
- Create custom exceptions for application-specific errors
"""

class CustomError(Exception):
    """Custom exception example"""
    def __init__(self, message, code=None):
        super().__init__(message)
        self.code = code

def error_handling_demo():
    """Demonstrate error handling"""
    print("\n--- Error Handling ---")
    
    # Basic try/except
    print("\nBasic exception handling:")
    try:
        result = 10 / 0
    except ZeroDivisionError as e:
        print(f"  Caught: {type(e).__name__}: {e}")
    
    # Multiple exceptions
    print("\nMultiple exception types:")
    test_values = [10, 0, "hello", 5]
    for val in test_values:
        try:
            result = 100 / val
            print(f"  100 / {val} = {result}")
        except ZeroDivisionError:
            print(f"  Can't divide by zero: {val}")
        except TypeError:
            print(f"  Type error with: {val}")
    
    # try/except/else/finally
    print("\ntry/except/else/finally:")
    try:
        result = 10 / 2
    except ZeroDivisionError:
        print("  Division by zero!")
    else:
        print(f"  No exception, result: {result}")
    finally:
        print("  This always runs")
    
    # Raising exceptions
    print("\nRaising exceptions:")
    try:
        age = -5
        if age < 0:
            raise ValueError(f"Age can't be negative: {age}")
    except ValueError as e:
        print(f"  Caught: {e}")
    
    # Custom exceptions
    print("\nCustom exceptions:")
    try:
        raise CustomError("Something went wrong", code=500)
    except CustomError as e:
        print(f"  Custom error: {e}, code: {e.code}")
    
    # Context manager for exception handling
    print("\nUsing contextlib.suppress:")
    from contextlib import suppress
    
    with suppress(FileNotFoundError):
        # This won't raise an error if file doesn't exist
        open("nonexistent_file.txt").close()
    print("  Handled missing file silently")


# =============================================================================
# 12. MODULES AND IMPORTS
# =============================================================================

"""
Modules and Imports in Python:

- Module: A .py file containing Python code
- Package: A directory with __init__.py and modules
- Import methods:
  - import module
  - from module import name
  - from module import name as alias
  - import module as alias
  - from module import *

Module Search Path:
- sys.path contains directories Python searches
- Current directory
- PYTHONPATH environment variable
- Standard library locations
- site-packages

Creating Modules:
- Any .py file is a module
- Use __name__ == "__main__" to run as script
- __init__.py for packages

Standard Library Modules:
- os: Operating system interface
- sys: System-specific parameters
- math: Mathematical functions
- datetime: Date and time
- json: JSON encoding/decoding
- re: Regular expressions
- collections: Specialized container types
- pathlib: Path manipulation
- ... and many more
"""

import math
import sys
from datetime import datetime, date, timedelta
from collections import Counter, defaultdict, namedtuple

def modules_demo():
    """Demonstrate module usage"""
    print("\n--- Modules and Imports ---")
    
    # Math module
    print("\n--- math module ---")
    print(f"math.pi: {math.pi}")
    print(f"math.e: {math.e}")
    print(f"math.sqrt(16): {math.sqrt(16)}")
    print(f"math.pow(2, 10): {math.pow(2, 10)}")
    print(f"math.floor(3.7): {math.floor(3.7)}")
    print(f"math.ceil(3.2): {math.ceil(3.2)}")
    print(f"math.sin(math.pi/2): {math.sin(math.pi/2)}")
    print(f"math.log10(100): {math.log10(100)}")
    
    # Datetime module
    print("\n--- datetime module ---")
    now = datetime.now()
    print(f"Current datetime: {now}")
    print(f"Date only: {now.date()}")
    print(f"Time only: {now.time()}")
    
    today = date.today()
    print(f"Today: {today}")
    print(f"Tomorrow: {today + timedelta(days=1)}")
    
    # Calculate age
    birth_date = date(1990, 5, 15)
    age = today.year - birth_date.year
    print(f"Age from birth date: {age}")
    
    # Collections module
    print("\n--- collections module ---")
    
    # Counter
    words = ["apple", "banana", "apple", "cherry", "banana", "apple"]
    word_count = Counter(words)
    print(f"Counter: {word_count}")
    print(f"Most common: {word_count.most_common(2)}")
    
    # defaultdict
    dd = defaultdict(list)
    dd["fruits"].append("apple")
    dd["fruits"].append("banana")
    dd["vegetables"].append("carrot")
    print(f"defaultdict: {dict(dd)}")
    
    # namedtuple
    Point = namedtuple('Point', ['x', 'y'])
    p = Point(10, 20)
    print(f"namedtuple Point: x={p.x}, y={p.y}")
    
    # sys module
    print("\n--- sys module ---")
    print(f"Python version: {sys.version}")
    print(f"Executable: {sys.executable}")
    print(f"Platform: {sys.platform}")
    print(f"Path entries: {len(sys.path)}")
    
    # This module's attributes
    print(f"\nCurrent module name: {__name__}")


# =============================================================================
# 13. LIST COMPREHENSIONS (Detailed)
# =============================================================================

"""
List Comprehensions - Detailed Guide:

Syntax:
[expression for item in iterable if condition]

Components:
- expression: What to do with each item
- for item in iterable: The iteration
- if condition: Optional filter

Examples:
- [x**2 for x in range(10)]  # Squares
- [x for x in numbers if x > 0]  # Filter
- [x*y for x in range(3) for y in range(3)]  # Nested

Advantages:
- More concise than for loops
- Often faster (implemented in C)
- More Pythonic

When NOT to use:
- Complex logic (use regular loop)
- When readability suffers
- When you need side effects
"""

def list_comprehensions_demo():
    """Demonstrate list comprehensions in detail"""
    print("\n--- List Comprehensions (Detailed) ---")
    
    # Basic comprehension
    print("\n1. Basic comprehension:")
    numbers = range(10)
    squares = [x ** 2 for x in numbers]
    print(f"   Squares: {squares}")
    
    # With condition
    print("\n2. With condition:")
    evens = [x for x in numbers if x % 2 == 0]
    odds = [x for x in numbers if x % 2 != 0]
    print(f"   Evens: {evens}")
    print(f"   Odds: {odds}")
    
    # With if-else
    print("\n3. With if-else (conditional expression):")
    labels = ["even" if x % 2 == 0 else "odd" for x in numbers]
    print(f"   Labels: {labels}")
    
    # Nested comprehension
    print("\n4. Nested comprehension:")
    matrix = [[i * j for j in range(5)] for i in range(5)]
    for row in matrix:
        print(f"   {row}")
    
    # Flattening nested lists
    print("\n5. Flattening:")
    nested = [[1, 2, 3], [4, 5], [6, 7, 8, 9]]
    flat = [item for sublist in nested for item in sublist]
    print(f"   Flattened: {flat}")
    
    # With string operations
    print("\n6. String operations:")
    words = ["hello", "world", "python", "programming"]
    upper_words = [word.upper() for word in words]
    word_lengths = [len(word) for word in words]
    print(f"   Upper: {upper_words}")
    print(f"   Lengths: {word_lengths}")
    
    # Dictionary and set comprehensions
    print("\n7. Dict and Set comprehensions:")
    dict_comp = {x: x ** 3 for x in range(5)}
    set_comp = {x % 3 for x in range(10)}
    print(f"   Dict: {dict_comp}")
    print(f"   Set: {set_comp}")
    
    # Filtering with multiple conditions
    print("\n8. Multiple conditions:")
    numbers = range(20)
    result = [x for x in numbers if x > 5 if x < 15 if x % 2 == 0]
    print(f"   5 < x < 15 and even: {result}")


# =============================================================================
# 14. BASIC OOP (Object-Oriented Programming)
# =============================================================================

"""
Basic OOP Concepts in Python:

Classes:
- Blueprint for creating objects
- Defined with 'class' keyword
- Can have attributes (data) and methods (functions)

Objects:
- Instance of a class
- Have state (attributes) and behavior (methods)

Key Concepts:
- __init__: Constructor method
- self: Reference to instance
- Inheritance: Create class from another class
- Polymorphism: Same interface, different implementations
- Encapsulation: Hide internal details
- Abstraction: Simplify complex reality

Special Methods (Magic/Dunder Methods):
- __str__: String representation for users
- __repr__: String representation for developers
- __len__: Length of object
- __eq__: Equality comparison
- __add__: Addition operator
- ... and many more

Properties:
- @property decorator for getter
- @attribute.setter for setter
- Allow methods to be accessed like attributes
"""

class Animal:
    """Base Animal class"""
    
    # Class attribute (shared by all instances)
    species_count = 0
    
    def __init__(self, name, age=0):
        """Constructor - initialize instance attributes"""
        self.name = name  # Instance attribute
        self.age = age
        Animal.species_count += 1
    
    def speak(self):
        """Method that can be overridden"""
        return f"{self.name} makes a sound"
    
    def info(self):
        """Return info about the animal"""
        return f"{self.name} is {self.age} years old"
    
    def __str__(self):
        """User-friendly string representation"""
        return f"Animal({self.name}, {self.age})"
    
    def __repr__(self):
        """Developer-friendly string representation"""
        return f"Animal(name='{self.name}', age={self.age})"


class Dog(Animal):
    """Dog class - inherits from Animal"""
    
    def __init__(self, name, age=0, breed="Unknown"):
        # Call parent constructor
        super().__init__(name, age)
        self.breed = breed
    
    def speak(self):
        """Override parent method"""
        return f"{self.name} says: Woof!"
    
    def fetch(self, item):
        """Dog-specific method"""
        return f"{self.name} fetches the {item}!"


class Cat(Animal):
    """Cat class - inherits from Animal"""
    
    def speak(self):
        """Override parent method"""
        return f"{self.name} says: Meow!"


class BankAccount:
    """Bank account with encapsulation"""
    
    def __init__(self, owner, balance=0):
        self.owner = owner
        self.__balance = balance  # Private attribute (name mangling)
    
    @property
    def balance(self):
        """Getter for balance"""
        return self.__balance
    
    @balance.setter
    def balance(self, value):
        """Setter with validation"""
        if value < 0:
            raise ValueError("Balance can't be negative")
        self.__balance = value
    
    def deposit(self, amount):
        """Deposit money"""
        if amount <= 0:
            raise ValueError("Amount must be positive")
        self.__balance += amount
        return self.__balance
    
    def withdraw(self, amount):
        """Withdraw money"""
        if amount <= 0:
            raise ValueError("Amount must be positive")
        if amount > self.__balance:
            raise ValueError("Insufficient funds")
        self.__balance -= amount
        return self.__balance
    
    def __str__(self):
        return f"BankAccount(owner={self.owner}, balance=${self.__balance:.2f})"


class Product:
    """Product class demonstrating special methods"""
    
    def __init__(self, name, price, quantity=1):
        self.name = name
        self.price = price
        self.quantity = quantity
    
    def __eq__(self, other):
        """Equality comparison"""
        if not isinstance(other, Product):
            return False
        return self.name == other.name
    
    def __lt__(self, other):
        """Less than comparison (by price)"""
        if not isinstance(other, Product):
            return NotImplemented
        return self.price < other.price
    
    def __len__(self):
        """Length (by quantity)"""
        return self.quantity
    
    def total_price(self):
        """Calculate total price"""
        return self.price * self.quantity
    
    def __str__(self):
        return f"Product({self.name}, ${self.price:.2f} x {self.quantity})"


def oop_demo():
    """Demonstrate basic OOP concepts"""
    print("\n--- Basic OOP ---")
    
    # Creating instances
    print("\n1. Creating Objects:")
    dog = Dog("Buddy", 3, "Golden Retriever")
    cat = Cat("Whiskers", 2)
    
    print(f"   Dog: {dog}")
    print(f"   Cat: {cat}")
    print(f"   Dog repr: {repr(dog)}")
    
    # Method calls
    print("\n2. Method Calls:")
    print(f"   Dog speaks: {dog.speak()}")
    print(f"   Cat speaks: {cat.speak()}")
    print(f"   Dog info: {dog.info()}")
    print(f"   Dog fetch: {dog.fetch('ball')}")
    
    # Inheritance
    print("\n3. Inheritance:")
    print(f"   Dog is Animal: {isinstance(dog, Animal)}")
    print(f"   Cat is Animal: {isinstance(cat, Animal)}")
    print(f"   Dog is Dog: {isinstance(dog, Dog)}")
    print(f"   Cat is Dog: {isinstance(cat, Dog)}")
    print(f"   Dog class: {type(dog).__name__}")
    
    # Encapsulation
    print("\n4. Encapsulation (BankAccount):")
    account = BankAccount("Alice", 1000)
    print(f"   Account: {account}")
    print(f"   Balance: ${account.balance:.2f}")
    account.deposit(500)
    print(f"   After deposit: ${account.balance:.2f}")
    account.withdraw(200)
    print(f"   After withdrawal: ${account.balance:.2f}")
    try:
        # Can't access private attribute directly
        print(f"   __balance: {account.__balance}")
    except AttributeError as e:
        print(f"   Can't access private attribute: {e}")
    
    # Special methods
    print("\n5. Special Methods (Product):")
    p1 = Product("Laptop", 999.99, 2)
    p2 = Product("Mouse", 29.99, 5)
    p3 = Product("Laptop", 999.99, 1)  # Same name, different quantity
    
    print(f"   p1: {p1}")
    print(f"   p2: {p2}")
    print(f"   p1 == p3 (by name): {p1 == p3}")
    print(f"   p1 < p2 (by price): {p1 < p2}")
    print(f"   p2 < p1: {p2 < p1}")
    print(f"   len(p1): {len(p1)}")
    print(f"   len(p2): {len(p2)}")
    
    # Sorting with special methods
    products = [p1, p2]
    products.sort()
    print(f"   Sorted by price: {[str(p) for p in products]}")


# =============================================================================
# MAIN EXECUTION
# =============================================================================

def main():
    """Run all demonstrations"""
    print("=" * 70)
    print("PYTHON BASIC - Comprehensive Guide to Core Python Features")
    print("=" * 70)
    
    variables_and_types_demo()
    operators_demo()
    control_flow_demo()
    functions_demo()
    strings_demo()
    lists_demo()
    tuples_demo()
    dictionaries_demo()
    sets_demo()
    file_io_demo()
    error_handling_demo()
    modules_demo()
    list_comprehensions_demo()
    oop_demo()
    
    print("\n" + "=" * 70)
    print("END OF PYTHON BASIC GUIDE")
    print("=" * 70)


if __name__ == "__main__":
    main()
