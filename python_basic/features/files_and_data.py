# -*- coding: utf-8 -*-
"""
Feature 23: Working with Files and Data
pathlib, JSON, CSV, ConfigParser, environment variables.
"""

def demo() -> None:
    print("\n" + "---" * 20)
    print("23. WORKING WITH FILES AND DATA")
    print("---" * 20)

    import csv
    import json
    import os
    import pathlib
    from configparser import ConfigParser

    current_dir: pathlib.Path = pathlib.Path.cwd()

    print("\n3.1 pathlib:")
    test_file: pathlib.Path = current_dir / "test_file.txt"
    test_file.write_text("Hello from pathlib!", encoding="utf-8")
    print(f"  Created file: {test_file.name}")
    print(f"  Is file: {test_file.is_file()}")
    print(f"  Read: {test_file.read_text(encoding='utf-8')}")
    test_file.unlink()
    print(f"  Deleted: {test_file.exists()}")

    print("\n3.2 JSON:")
    data: dict = {
        "name": "Alice",
        "age": 30,
        "is_student": False,
        "courses": ["Math", "Science"]
    }

    json_str: str = json.dumps(data, indent=2)
    print(f"  JSON: {json_str}")

    parsed: dict = json.loads(json_str)
    print(f"  Parsed name: {parsed['name']}")

    json_file: pathlib.Path = current_dir / "data.json"
    json_file.write_text(json_str, encoding="utf-8")
    print(f"  Wrote JSON to: {json_file.name}")

    loaded: dict = json.loads(json_file.read_text(encoding="utf-8"))
    print(f"  Loaded JSON: {loaded['name']}")
    json_file.unlink()

    print("\n3.3 CSV:")
    csv_file: pathlib.Path = current_dir / "data.csv"
    with open(csv_file, "w", newline="", encoding="utf-8") as f:
        writer: csv.writer = csv.writer(f)
        writer.writerow(["Name", "Age", "City"])
        writer.writerow(["Alice", 30, "New York"])
        writer.writerow(["Bob", 25, "Boston"])

    print("  Wrote CSV file:")
    with open(csv_file, "r", encoding="utf-8") as f:
        reader: csv.reader = csv.reader(f)
        for row in reader:
            print(f"    {row}")
    csv_file.unlink()

    print("\n3.4 ConfigParser:")
    config_file: pathlib.Path = current_dir / "config.ini"
    config_content: str = """
[development]
database_host = localhost
database_port = 5432
debug = True

[production]
database_host = prod.db.example.com
database_port = 5432
debug = False
"""

    config_file.write_text(config_content, encoding="utf-8")
    print(f"  Wrote config file to: {config_file.name}")

    config: ConfigParser = ConfigParser()
    config.read(config_file)

    print(f"  Development database host: {config['development']['database_host']}")
    print(f"  Production server port: {config['production']['database_port']}")
    config_file.unlink()

    print("\n3.5 Environment Variables:")
    os.environ["APP_ENV"] = "production"
    print(f"  APP_ENV: {os.environ.get('APP_ENV', 'not set')}")
    print(f"  user: {os.environ.get('USER', 'not set')}")

    for f in [test_file, csv_file]:
        if f.exists():
            f.unlink()
