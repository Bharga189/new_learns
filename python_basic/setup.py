#!/usr/bin/env python
"""
Setup script for Python Basic project.
Used to install the package and its dependencies.
"""

from setuptools import setup, find_packages

setup(
    name="python-basic",
    version="1.0.0",
    description="Comprehensive guide to core Python programming features",
    long_description=open("README.md").read(),
    long_description_content_type="text/markdown",
    author="Python Basic Project",
    author_email="admin@example.com",
    url="https://github.com/example/python-basic",
    packages=find_packages(),
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Intended Audience :: Education",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Topic :: Education",
        "Topic :: Software Development",
    ],
    python_requires=">=3.8",
    keywords="python tutorial fundamentals oop data-structures",
    project_urls={
        "Documentation": "https://github.com/example/python-basic/blob/main/README.md",
        "Source": "https://github.com/example/python-basic",
        "Tracker": "https://github.com/example/python-basic/issues",
    },
    install_requires=[],
    extras_require={
        "dev": [
            "pytest>=6.0",
            "black>=21.0",
            "flake8>=3.9",
            "mypy>=0.910",
        ],
        "docs": [
            "sphinx>=4.0",
            "sphinx-rtd-theme>=0.5",
        ],
    },
    entry_points={
        "console_scripts": [
            "python-basic=main:main",
        ],
    },
)
