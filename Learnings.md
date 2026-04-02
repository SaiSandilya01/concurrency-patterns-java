
Race Condition	- Shared data corrupted by unsynchronized access
Deadlock	- Two threads wait on each other forever
Starvation	- A thread never gets CPU time


How synchronized works
Every Java object has a hidden built-in lock called a monitor. When you mark a method synchronized, Java does this automatically:

Thread enters increment()
    → tries to acquire the monitor lock on `this` (the Counter object)
    → if lock is FREE: acquires it, runs the method, releases it
    → if lock is TAKEN: BLOCKS and waits until the current holder releases it


Two key features synchronized lacks:
1. Fairness mode ← directly tackles starvation

java
ReentrantLock lock = new ReentrantLock(true); // fair = true
With synchronized, when the lock is released, the JVM picks any waiting thread — the same thread could keep getting skipped. With a fair ReentrantLock, threads are granted the lock in FIFO order (first come, first served). No thread starves.

2. tryLock() with timeout ← tackles a different problem: indefinite blocking

java
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try { /* do work */ } 
    finally { lock.unlock(); }
} else {
    // gave up — do something else instead of waiting forever
}
With synchronized, a thread blocks forever waiting. tryLock() lets it give up and move on.

---

# Concurrent Programming — Full Interview Guide

## Table of Contents
1. [Core Problems](#1-core-problems)
2. [Race Condition](#2-race-condition)
3. [Synchronization — Deeper Dive](#3-synchronization--deeper-dive)
4. [Deadlock](#4-deadlock)
5. [Producer-Consumer](#5-producer-consumer)
6. [Semaphores](#6-semaphores)
7. [Readers-Writers](#7-readers-writers)
8. [Golden Rules](#8-golden-rules)

---

## 1. Core Problems

| Problem | Description |
|---|---|
| **Race Condition** | Shared data corrupted by unsynchronized access |
| **Deadlock** | Two or more threads wait on each other forever |
| **Starvation** | A thread never gets CPU time |

---

## 2. Race Condition

### Why does `count++` fail in multithreading?
**Doubt asked:** *"The result is not 2000 — is it because the entire process is still not complete?"*

No — `join()` ensures threads finish before printing. The count is genuinely wrong due to a race condition.

`count++` looks like one operation but is actually **three CPU steps**:
```
1. READ  → load count from memory
2. ADD   → increment by 1
3. WRITE → store back to memory
```

Two threads can interleave these steps:
```
Thread 1: READ(100) → ADD → WRITE(101)
Thread 2: READ(100) → ADD → WRITE(101)   ← overwrites Thread 1's result!
Expected: 102.  Actual: 101
```

### Key interview answer
> "`count++` is not thread-safe because it is a **read-modify-write** operation — three CPU instructions, not one atomic operation."

### Fixes
```java
// Option 1: synchronized
public synchronized void increment() { count++; }

// Option 2: AtomicInteger — hardware-level atomic, no lock needed
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();
```

---

## 3. Synchronization — Deeper Dive

### `synchronized` Method vs Block

**Method** — locks on `this`:
```java
public synchronized void increment() { count++; }
```

**Block** — lock any object, protect only what's necessary:
```java
public void increment() {
    synchronized(this) { count++; }
}
```

**Doubt asked:** *"How does `synchronized()` work?"*

Every Java object has a hidden **monitor lock**. `synchronized(obj)` acquires that object's monitor before entering the block, and releases it on exit. If another thread holds it, the incoming thread blocks.

| | `synchronized` method | `synchronized(obj)` block |
|---|---|---|
| Lock target | Always `this` or Class | Any object you choose |
| Scope | Entire method | Just the block |
| Flexibility | Low | High |
| Performance | Holds lock longer | Can minimize lock duration |

### `synchronized` vs `ReentrantLock`

**Doubt asked:** *"How does ReentrantLock fix starvation?"*

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Fairness | ❌ JVM picks any waiting thread | ✅ `new ReentrantLock(true)` → FIFO order |
| Timeout | ❌ Blocks forever | ✅ `tryLock(500, MILLISECONDS)` |
| Auto-release | ✅ Always releases | ❌ Must call `unlock()` manually |
| Simplicity | ✅ Cleaner | ❌ More verbose |

> ⚠️ Always use `try { } finally { lock.unlock(); }` with `ReentrantLock` — forgetting `unlock()` causes deadlock.

---

## 4. Deadlock

### What is it?
Two threads each hold a lock the other needs. Neither can proceed. Ever.

```
Thread A: holds Lock1, waits for Lock2
Thread B: holds Lock2, waits for Lock1  → Circular wait → Deadlock
```

**Doubt asked:** *"Is the fix to run everything on a single thread?"*

No — that defeats concurrency. The fix is to keep both threads running but make them agree on lock order.

### The 4 Coffman Conditions
All four must be true for deadlock to occur. Break any one and deadlock is impossible.

| Condition | Meaning |
|---|---|
| **Mutual Exclusion** | A resource can only be held by one thread |
| **Hold & Wait** | Thread holds one lock while waiting for another |
| **No Preemption** | Locks can't be forcibly taken away |
| **Circular Wait** | A waits for B, B waits for A |

### Classic Example: Bank Transfer
```java
// BROKEN — Thread A locks A then B; Thread B locks B then A → deadlock
synchronized(this)  { synchronized(other) { ... } }
synchronized(other) { synchronized(this)  { ... } }
```

### The Fix: Consistent Lock Ordering
```java
// FIXED — always lock the lower ID first, regardless of direction
BankAccount first  = this.id < other.id ? this : other;
BankAccount second = this.id < other.id ? other : this;
synchronized(first) {
    synchronized(second) { /* transfer */ }
}
```
Both threads always knock on the same door first → circular wait is broken.

---

## 5. Producer-Consumer

### The Problem
Producer adds items to a bounded buffer; Consumer removes them.
- Producer **waits** when buffer is full
- Consumer **waits** when buffer is empty

### Tools: `wait()` and `notify()`
| Method | What it does |
|---|---|
| `wait()` | Releases the lock and sleeps the thread |
| `notify()` | Wakes one thread waiting on the same object |
| `notifyAll()` | Wakes all waiting threads |

> Must be called **inside a `synchronized` block** — otherwise `IllegalMonitorStateException`.

### Implementation
```java
synchronized void produce(int item) throws InterruptedException {
    while (queue.size() == capacity) { wait(); }  // buffer full — sleep
    queue.offer(item);
    notify();  // wake consumer
}

synchronized void consume() throws InterruptedException {
    while (queue.isEmpty()) { wait(); }  // buffer empty — sleep
    queue.poll();
    notify();  // wake producer
}
```

### ⚠️ Why `while`, not `if`?
**Doubt asked:** *"Help me internalize why `while` instead of `if`."*

Imagine 2 consumers, buffer has 1 item. Both are sleeping in `wait()`. Producer adds an item and calls `notifyAll()`. **Both** consumers wake up. Consumer 1 gets the lock, takes the item. Consumer 2 gets the lock next:
- With `if` → skips the check, tries to consume from empty queue → **crash**
- With `while` → re-checks, sees queue is empty, goes back to sleep ✅

Threads can also wake up **spuriously** (for no reason — documented JVM behavior). `while` guards against this.

> **Rule: `wait()` inside `while`, never `if`.**

### `notify()` vs `notifyAll()`
- One producer + one consumer → `notify()` is fine
- Multiple producers or consumers → use `notifyAll()` to avoid waking the wrong thread type

---

## 6. Semaphores

### What is a Semaphore?
A counter with two operations:

| Operation | Effect |
|---|---|
| `acquire()` | Decrements counter. Blocks if already 0 |
| `release()` | Increments counter. Wakes a waiting thread |

Think: **parking lot with N spots** — cars wait when full, enter when space opens.

```java
Semaphore sem = new Semaphore(3);  // 3 concurrent threads allowed
sem.acquire();   // take a permit (blocks if 0) — handles waiting internally!
// ... do work ...
sem.release();   // return a permit
```

> ⚠️ **Do NOT add `synchronized` on top of Semaphore** — it already handles its own blocking. Adding `synchronized` causes deadlock (as we discovered!).

### Semaphore vs Queue
**Doubt asked:** *"What is the difference between a semaphore and a queue?"*

| | Queue | Semaphore |
|---|---|---|
| Purpose | Stores **data** between threads | Controls **access** to a resource |
| Holds | Items (tasks, messages) | A count (permits) |
| Example | Task queue | Connection pool |

> Analogy: Queue = the line of customers. Semaphore = the number of open teller windows. Both are often used together.

### `synchronized` vs `Semaphore(N)`
| | `synchronized` | `Semaphore(N)` |
|---|---|---|
| Concurrency | **1 thread** at a time | **N threads** at a time |
| Use case | Mutual exclusion | Resource pool limiting |

---

## 7. Readers-Writers

### The Problem
Multiple threads read/write a shared resource:

| Situation | Allowed? |
|---|---|
| Multiple readers simultaneously | ✅ Safe — no mutation |
| Reader + Writer simultaneously | ❌ Dirty read |
| Multiple writers simultaneously | ❌ Data corruption |

`synchronized` is too conservative — blocks all threads even when parallel reading is safe.

### State to track
```java
int activeReaders = 0;      // how many readers are currently reading
boolean writerActive = false; // is a writer currently writing?
```

### Implementation
```java
synchronized void startRead() throws InterruptedException {
    while (writerActive) { wait(); }  // wait if a writer is active
    activeReaders++;
}

synchronized void endRead() {
    activeReaders--;
    if (activeReaders == 0) { notifyAll(); }  // last reader wakes waiting writers
}

synchronized void startWrite() throws InterruptedException {
    while (activeReaders > 0 || writerActive) { wait(); }  // wait for everyone to finish
    writerActive = true;
}

synchronized void endWrite() {
    writerActive = false;
    notifyAll();  // wake both readers and writers
}
```

### Common mistakes made
- `endRead()` should **decrement** `activeReaders` — not set `writerActive = true`
- `startWrite()` condition is `writerActive == true` (wait while active), not `== false`
- `endWrite()` must call `notifyAll()` — both readers and writers may be waiting

---

## 8. Golden Rules

1. **`wait()` always inside a `while` loop**, never `if`
2. **`wait()`/`notify()` always inside `synchronized`**
3. **Acquire multiple locks in the same global order** to prevent deadlock
4. **Release locks in `finally` blocks** when using `ReentrantLock`
5. **Semaphore handles its own blocking** — don't wrap in `synchronized`
6. **`count++` is NOT atomic** — use `synchronized` or `AtomicInteger`

## Quick Cheat Sheet

```
Race Condition    → synchronized method/block or AtomicInteger
Deadlock          → consistent lock ordering (lowest ID first)
Producer-Consumer → wait()/notifyAll() in while loop + bounded buffer
Semaphore         → N concurrent threads, acquire()/release()
Readers-Writers   → activeReaders counter + writerActive flag
Thread Pool       → task queue + N worker threads looping on wait()/notifyAll()
```

---

## 8. Thread Pool

### What is it?
Creating a new thread per task is expensive. A thread pool maintains a fixed set of **pre-created worker threads** that pick up tasks from a shared queue.

```
Task1 ──┐
Task2 ──┤──► [Queue] ──► Worker1
Task3 ──┤              ► Worker2
Task4 ──┘              ► Worker3
```

Workers loop forever, picking up tasks. No thread creation overhead per task.

### Key design decisions

**Backpressure** — block `submit()` if queue is too full:
```java
while (taskQueue.size() >= workers.length) { wait(); }
```

**Worker loop**:
```java
while (true) {
    Runnable task;
    synchronized (threadPool) {
        while (taskQueue.isEmpty() && !isShutdown) { wait(); }
        if (isShutdown && taskQueue.isEmpty()) return;  // graceful exit
        task = taskQueue.poll();
        notifyAll();  // wake blocked submit()
    }
    task.run();  // ← OUTSIDE the lock! otherwise only 1 worker runs at a time
}
```

**Shutdown**:
```java
isShutdown = true;
notifyAll();  // wake all sleeping workers so they can see the flag and exit
```

### Common mistakes
- Running `task.run()` **inside** the synchronized block — kills concurrency, only 1 worker active at a time
- Using `notify()` instead of `notifyAll()` after polling — may wake the wrong thread
- Not handling the shutdown flag — workers loop forever after shutdown

### Wait conditions are mutually exclusive
- Workers wait when queue is **empty**
- `submit()` waits when queue is **full**
- These can never both be true → `notify()` would be safe, but `notifyAll()` is safer practice

### Java's built-in (after you build it yourself)
```java
ExecutorService pool = Executors.newFixedThreadPool(3);
pool.submit(() -> System.out.println("task"));
pool.shutdown();
```

---

## 9. Dining Philosophers

### The Problem
5 philosophers sit around a table. Between each pair is 1 fork. To eat, a philosopher needs **both** the left and right forks.

If everyone picks up their left fork at the exact same time, they all wait forever for their right fork. **Circular Wait = Deadlock.**

### Solution 1: Lock Ordering (The Interview Standard)
Just like Bank Transfer, force a global ordering. Instead of picking "left then right", pick the **lower numbered fork first**.

```java
int firstFork = Math.min(leftFork, rightFork);
int secondFork = Math.max(leftFork, rightFork);

synchronized (forks[firstFork]) {
    synchronized (forks[secondFork]) {
        // eat
    }
}
```
*Why it works:* The last philosopher (id 4) wants forks 4 and 0. Instead of picking 4 first, they pick 0 first. Since Philosopher 0 already has fork 0, Philosopher 4 is blocked immediately, breaking the circle and letting Philosopher 3 eat.

### Solution 2: The Arbitrator / Waiter (Your Custom Structural Fix)
Introduce a central "Waiter" who controls the forks. A philosopher asks the Waiter for *both* forks at once. 
The Waiter only hands them over if both are free.

```java
class Waiter {
    boolean[] forks = new boolean[5];
    
    public synchronized void pickUpForks(int id) throws InterruptedException {
        int left = id, right = (id + 1) % 5;
        // Wait until BOTH are free
        while (forks[left] || forks[right]) { wait(); } 
        forks[left] = forks[right] = true;
    }
    
    public synchronized void putDownForks(int id) {
        int left = id, right = (id + 1) % 5;
        forks[left] = forks[right] = false;
        // Wake up everybody who is waiting for forks
        notifyAll(); 
    }
}
```
*Why it works:* Forks are never acquired one-by-one. It's an atomic "all or nothing" grab. This naturally allows up to `floor(n/2)` non-adjacent philosophers to eat simultaneously. This perfectly prevents deadlock and is very elegant.

---

## 10. Advanced Concurrency Tools (Rate Limiter Learnings)

### CountDownLatch (The Stampede Generator)
Used to make threads wait for a specific condition or synchronize their starting times.
- `new CountDownLatch(1)` acts as a red traffic light. 
- `latch.await()` makes threads sleep frozen at the starting line.
- `latch.countDown()` turns the light green, waking all threads instantly. 
**Use Case:** Excellent for stress-testing **race conditions** by hammering an endpoint with 100 threads at the exact same millisecond.

### ConcurrentHashMap.computeIfAbsent() vs Global Locks
A common anti-pattern to prevent a "Check-Then-Act" race condition is locking an entire object:
```java
// ❌ BAD: Global Bottleneck. Entire app is single-threaded here!
synchronized(this) { 
    if (!map.containsKey("User")) { 
        map.put("User", new Bucket()); 
    }
}
```

The elegant fix is `map.computeIfAbsent(key, mappingFunction)`.
1. **Thread-Safe Check-Then-Act**: It atomically checks for the key and puts the value if missing.
2. **Fragmented Locking**: A `ConcurrentHashMap` doesn't use one giant lock. It divides its memory into "segments". When you run `computeIfAbsent`, it only locks the tiny fraction of the Map where that user lives. Thread A and Thread B can insert totally different keys simultaneously without waiting for each other!
3. **Lazy Evaluation (`k -> new Bucket()`)**: Why the weird arrow syntax? If we wrote `computeIfAbsent(key, new Bucket())`, Java would instantiate a brand new Bucket object on every single request, even if the user was already in the map, wasting massive CPU and memory. The lambda arrow tells Java: *"Here are instructions. ONLY run them if the user isn't in the map yet."*

### HashMap vs ConcurrentHashMap (Speed vs Safety)
- **`HashMap` (Single-Threaded):** Zero locking overhead, so it runs at maximum CPU speeds. Use it 95% of the time when passing data locally or parsing objects where only a single thread will ever access the map. *(Note: If multiple threads write to it simultaneously, the data structure corrupts).*
- **`ConcurrentHashMap` (Multi-Threaded):** Contains internal memory overhead for lock management. Use it strictly for **Shared State** across the server (like caches, connection pools, and Rate Limiters). It is entirely thread-safe but strictly forbids `null` keys and `null` values.
