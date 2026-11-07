# -*- coding: utf-8 -*-
"""
Feature 14: Working with Files and Data
pathlib, JSON, CSV, Pickle, ConfigParser, environment variables.
"""

import csv
import json
import os
from pathlib import Path


def files_and_data_demo() -> None:
    print("\n" + "=" * 70)
    print("14. WORKING WITH FILES AND DATA")
    print("=" * 70)

    # Pathlib
    print("\n14.1 pathlib:")
    current_dir = Path.cwd()
    print(f"  Current directory: {current_dir}")

    test_file = current_dir / "test_file.txt"
    test_file.write_text("Hello from pathlib!", encoding='utf-8')
    print(f"  Created file: {test_file.name}")
    print(f"  Is file: {test_file.is_file()}")
    print(f"  Read: {test_file.read_text(encoding='utf-8')}")
    test_file.unlink()
    print(f"  Deleted: {test_file.exists()}")

    # JSON
    print("\n14.2 JSON:")
    data = {
        "name": "Alice",
        "age": 30,
        "is_student": False,
        "courses": ["Math", "Science"]
    }

    json_str = json.dumps(data, indent=2)
    print(f"  JSON: {json_str}")

    # Parse JSON
    parsed = json.loads(json_str)
    print(f"  Parsed name: {parsed['name']}")

    # Read/write JSON file
    json_file = current_dir / "data.json"
    json_file.write_text(json_str, encoding='utf-8')
    print(f"  Wrote JSON to: {json_file.name}")

    loaded = json.loads(json_file.read_text(encoding='utf-8'))
    print(f"  Loaded JSON: {loaded['name']}")
    json_file.unlink()

    # CSV
    print("\n14.3 CSV:")
    csv_file = current_dir / "data.csv"
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['Name', 'Age', 'City'])
        writer.writerow(['Alice', 30, 'New York'])
        writer.writerow(['Bob', 25, 'Boston'])

    print("  Wrote CSV file:")
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            print(f"    {row}")
    csv_file.unlink()

    # Environment variables
    print("\n14.4 Environment Variables:")
    os.environ['MY_APP_CONFIG'] = "production"
    config = os.getenv('MY_APP_CONFIG', 'development')
    print(f"  Config: {config}")

    # ConfigParser
    print("\n14.5 ConfigParser:")
    config_file = current_dir / "config.ini"
    config_content = """[database]
host = localhost
port = 5432

[server]
host = 0.0.0.0
port = 8080
"""
    config_file.write_text(config_content, encoding='utf-8')

    import configparser
    config = configparser.ConfigParser()
    config.read(config_file)

    print(f"  Database host: {config['database']['host']}")
    print(f"  Server port: {config['server']['port']}")

    config_file.unlink()


if __name__ == "__main__":
    files_and_data_demo()
