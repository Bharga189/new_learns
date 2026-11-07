# -*- coding: utf-8 -*-
"""
Feature 19: Async Programming
async/await, asyncio, coroutines, tasks, gather, async context managers.
"""

import asyncio


async def async_task(name: str, delay: float) -> str:
    print(f"  Task {name} starting")
    await asyncio.sleep(delay)
    print(f"  Task {name} completed")
    return f"Result from {name}"


async def run_basic() -> None:
    print("  Hello from async function")
    await asyncio.sleep(0.1)
    print("  World")


async def run_tasks() -> list:
    task1 = asyncio.create_task(async_task("A", 1))
    task2 = asyncio.create_task(async_task("B", 2))
    task3 = asyncio.create_task(async_task("C", 0.5))
    results: list = await asyncio.gather(task1, task2, task3)
    return results


async def sequential() -> str:
    start: float = asyncio.get_event_loop().time()
    await async_task("1", 1)
    await async_task("2", 1)
    await async_task("3", 1)
    return f"Sequential took {asyncio.get_event_loop().time() - start:.2f}s"


async def concurrent() -> str:
    start: float = asyncio.get_event_loop().time()
    await asyncio.gather(
        async_task("1", 1),
        async_task("2", 1),
        async_task("3", 1)
    )
    return f"Concurrent took {asyncio.get_event_loop().time() - start:.2f}s"


async def use_resource() -> None:
    class AsyncResource:
        async def __aenter__(self) -> "AsyncResource":
            print("  Acquiring async resource")
            await asyncio.sleep(0.1)
            return self

        async def __aexit__(self, exc_type, exc_val, exc_tb) -> None:
            print("  Releasing async resource")
            await asyncio.sleep(0.1)

    async with AsyncResource() as res:
        print("  Using resource")


async def run() -> None:
    print("\n" + "---" * 20)
    print("19. ASYNC PROGRAMMING")
    print("---" * 20)

    print("\n1.1 Basic Async Function:")
    await run_basic()

    print("\n1.2 Running Multiple Tasks:")
    results: list = await run_tasks()
    print(f"  Results: {results}")

    print("\n1.3 Sequential vs Concurrent:")
    seq_result: str = await sequential()
    conc_result: str = await concurrent()
    print(f"  {seq_result}")
    print(f"  {conc_result}")

    print("\n1.4 Async Context Manager:")
    await use_resource()
