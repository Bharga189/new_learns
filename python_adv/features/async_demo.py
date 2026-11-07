# -*- coding: utf-8 -*-
"""
Feature 8: Async Programming
async/await, asyncio, coroutines, tasks, gather, async context managers.
"""

import asyncio


async def async_task(name: str, delay: float) -> str:
    """Async task that simulates work"""
    print(f"  Task {name} starting")
    await asyncio.sleep(delay)
    print(f"  Task {name} completed")
    return f"Result from {name}"


async def fetch_data(url: str) -> str:
    """Simulate fetching data from URL"""
    print(f"  Fetching {url}")
    await asyncio.sleep(1)  # Simulate network delay
    return f"Data from {url}"


async def async_demo() -> None:
    print("\n" + "=" * 70)
    print("8. ASYNC PROGRAMMING")
    print("=" * 70)

    # Basic async function
    print("\n8.1 Basic Async Function:")
    async def main():
        print("  Hello from async function")
        await asyncio.sleep(0.1)
        print("  World")
    await main()

    # Running multiple tasks
    print("\n8.2 Running Multiple Tasks:")
    async def run_tasks():
        # Create tasks
        task1 = asyncio.create_task(async_task("A", 1))
        task2 = asyncio.create_task(async_task("B", 2))
        task3 = asyncio.create_task(async_task("C", 0.5))
        # Wait for all
        results = await asyncio.gather(task1, task2, task3)
        return results

    results = await run_tasks()
    print(f"  Results: {results}")

    # Sequential vs concurrent
    print("\n8.3 Sequential vs Concurrent:")
    async def sequential():
        """Sequential execution"""
        start = asyncio.get_event_loop().time()
        await async_task("1", 1)
        await async_task("2", 1)
        await async_task("3", 1)
        return f"Sequential took {asyncio.get_event_loop().time() - start:.2f}s"

    async def concurrent():
        """Concurrent execution"""
        start = asyncio.get_event_loop().time()
        await asyncio.gather(
            async_task("1", 1),
            async_task("2", 1),
            async_task("3", 1)
        )
        return f"Concurrent took {asyncio.get_event_loop().time() - start:.2f}s"

    seq_result = await sequential()
    conc_result = await concurrent()
    print(f"  {seq_result}")
    print(f"  {conc_result}")

    # Async context manager
    print("\n8.4 Async Context Manager:")
    class AsyncResource:
        async def __aenter__(self):
            print("  Acquiring async resource")
            await asyncio.sleep(0.1)
            return self

        async def __aexit__(self, exc_type, exc_val, exc_tb):
            print("  Releasing async resource")
            await asyncio.sleep(0.1)

    async def use_resource():
        async with AsyncResource() as res:
            print("  Using resource")

    await use_resource()


if __name__ == "__main__":
    asyncio.run(async_demo())
