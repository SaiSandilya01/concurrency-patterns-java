package DistributedLock;

import java.util.Collections;

/**
 * Note: Uses our dummy Jedis class representing the connection to a Redis cluster.
 */
public class RedisDistributedLock {

    private final Jedis jedis;
    private final String lockKey;     // E.g. "CRON_JOB_LOCK"
    private final int lockTimeoutMs;  // Prevent deadlocks if the server crashes!

    private static final String RELEASE_LUA_SCRIPT =
        "local current_owner = redis.call('GET', KEYS[1])\n" +
        "if current_owner == ARGV[1] then\n" +
        "    return redis.call('DEL', KEYS[1])\n" +
        "end\n" +
        "return 0\n";

    public RedisDistributedLock(Jedis jedis, String lockKey, int lockTimeoutMs) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    /**
     * Tries to acquire the distributed lock for this specific server node.
     * 
     * @param nodeIdentifier A unique ID for this specific thread/server (e.g. UUID)
     * @return true if lock acquired successfully, false if someone else holds it
     */
    public boolean tryAcquireLock(String nodeIdentifier) {
        String result = jedis.set(lockKey, nodeIdentifier, "NX", "PX", lockTimeoutMs);

        if ("OK".equals(result)) {
            return true;
        }

        return false;
    }

    /**
     * Releases the lock, but ONLY if this node still owns it!
     */
    public void releaseLock(String nodeIdentifier) {
        jedis.eval(RELEASE_LUA_SCRIPT, Collections.singletonList(lockKey), Collections.singletonList(nodeIdentifier));
    }

    // ---------------------------------------------------------
    // Dummy Jedis Interface (Simulating a real Redis Server in memory!)
    // ---------------------------------------------------------
    static class Jedis {
        private final java.util.concurrent.ConcurrentHashMap<String, String> redisMemory = new java.util.concurrent.ConcurrentHashMap<>();

        public String set(String key, String value, String nxxx, String expx, long time) {
            // Simulating Redis SETNX!
            if ("NX".equals(nxxx)) {
                if (redisMemory.putIfAbsent(key, value) == null) {
                    return "OK";
                }
            }
            return null; // Key already exists!
        }

        public Object eval(String script, java.util.List<String> keys, java.util.List<String> args) {
            // Simulating the Lua Script Engine!
            String key = keys.get(0);
            String nodeTryingToDelete = args.get(0);
            
            String currentOwner = redisMemory.get(key);
            if (nodeTryingToDelete.equals(currentOwner)) {
                redisMemory.remove(key); // Secure Delete
                return 1L;
            }
            return 0L; // Identity theft prevented!
        }
    }
}
