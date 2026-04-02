# Concurrent Designs & Patterns in Java

This repository is a comprehensive study and implementation of core and advanced concurrent programming design patterns in Java. It explores how to identify, triage, and solve multithreading hazards.

## Core Hazards Addressed
- **Race Conditions:** Corrupted shared memory due to unsynchronized read-modify-write CPU instructions (see `Counter.java`).
- **Deadlocks:** The Coffman Conditions and circular wait states, addressed via strict Lock Ordering techniques (see `BankTransfer.java`).
- **Starvation:** Addressed through fairness flags and ReentrantLocks.

## Classic Synchronization Problems
- **Producer-Consumer:** Utilizing `wait()` and `notifyAll()` within while-loops to safely coordinate thread hand-offs.
- **Readers-Writers:** Balancing massive parallel read throughput while enforcing mutually exclusive write privileges.
- **Dining Philosophers:** Resolving complex circular-wait bottlenecks utilizing Global Lock Ordering and central "Waiter" arbitration strategies.

## Advanced Real-World Architectures
- **Custom Thread Pools:** Demonstrating `ExecutorService` internals by managing shared task queues and continuous worker loops.
- **Connection Pools:** utilizing `Semaphore` logic to accurately constrain and marshal access to expensive shared resources without explicit `synchronized` locks.
- **Rate Limiters:** Highly scalable, lock-fragmented architectures for bounding API traffic. Includes `FixedWindow` and `TokenBucket` algorithm implementations with specialized `ConcurrentHashMap` handling.

## Central Learnings Guide
A detailed master guide covering all underlying theory, memory models, JVM locking optimizations, and concurrency principles can be found in [Learnings.md](Learnings.md).
