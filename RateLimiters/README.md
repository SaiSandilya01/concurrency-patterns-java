# Rate Limiter Algorithms

This directory contains thread-safe implementations of two industry-standard rate limiting algorithms in Java, designed to handle high concurrency without introducing global bottlenecks. 

## Included Algorithms

### 1. Fixed Window Rate Limiter
The Fixed Window algorithm divides time into discrete, absolute boundaries (e.g., exactly 12:00:00 to 12:01:00). A user is permitted N requests during that window. Once the boundary passes, the counters are flushed.

**Key Concurrency Patterns Used:**
- **Double-Checked Locking:** Utilized to safely flush the shared map at window boundaries. 
- **AtomicInteger:** Used to safely increment request counts lock-free.

**Known Flaws:**
- Susceptible to the "Burst at Boundary" problem, where a system can accept up to 2N requests if N requests arrive in the final millisecond of Window A and N requests arrive in the first millisecond of Window B.

### 2. Token Bucket Rate Limiter
The Token Bucket algorithm resolves the boundary burst issue. It assigns a bucket with a maximum capacity (handling large momentary bursts) and a sustained token refill rate (managing steady traffic).

**Key Concurrency Patterns Used:**
- **Lazy Refill (Mathematical Scaling):** Time differential math is used to dispense tokens retroactively, removing the requirement for expensive background thread polling.
- **Fragmented Locking:** Global wait states are eliminated. `synchronized(this)` is strictly limited to the inner `UserBucket` class, allowing distinct users to process in parallel while synchronizing spammers locally.

## Important Java Concurrency Principles Handled

### Defeating the "Check-Then-Act" Race Condition
A major pitfall in concurrent programming involves checking state inside a shared Map, generating a decision, and writing to the Map. During this sequence, another thread can interleave and duplicate the creation process.

*Anti-Pattern:* 
```java
if (!users.containsKey("Bob")) {
    users.put("Bob", new UserBucket());
}
```

*Solution:* Utilizing `ConcurrentHashMap.computeIfAbsent()`. This provides memory-level synchronization on the exact hash bucket being manipulated, safely executing lambda evaluations only if the data is genuinely absent. This prevents global locking bottlenecks that would otherwise serialize the entire server.

### 3. Distributed Redis Rate Limiter
The above algorithms execute inside the single JVM heap space. For distributed architectures where multiple load-balanced Java servers exist, they are unviable due to the "Read-Modify-Write" gap across the network.
- **The Solution (Lua Scripts):** `FixedWindow.lua` provides a thread-safe, atomic execution layer processed natively inside the Redis DB engine. `RedisRateLimiter.java` leverages libraries like `jedis` to dynamically inject the Lua script avoiding any Java synchronization issues!
