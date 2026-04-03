import java.util.Collections;
import java.util.Arrays;

public class RedisRateLimiter implements RateLimiter {

    // Pseudo-class representing the Redis Client connection
    private final Jedis jedis;
    private final int limit;
    private final long windowSizeMs;

    private static final String LUA_SCRIPT = "local current_count = redis.call('GET', KEYS[1])\n" +
            "if current_count and tonumber(current_count) >= tonumber(ARGV[1]) then\n" +
            "    return 0\n" +
            "end\n" +
            "redis.call('INCR', KEYS[1])\n" +
            "if current_count == false then\n" +
            "    redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[2]))\n" +
            "end\n" +
            "return 1\n";

    public RedisRateLimiter(Jedis jedis, int limit, long windowSizeMs) {
        this.jedis = jedis;
        this.limit = limit;
        this.windowSizeMs = windowSizeMs;
    }

    @Override
    public boolean allowRequest(String userId) {
        String key = "rate_limit:" + userId;
        Object result = jedis.eval(LUA_SCRIPT, Collections.singletonList(key),
                Arrays.asList(String.valueOf(limit), String.valueOf(windowSizeMs)));
        return (Long) result == 1L;
    }

    // Dummy Jedis
    static class Jedis {
        public Object eval(String script, java.util.List<String> keys, java.util.List<String> args) {
            System.out.println("Executing Lua Script inside Redis Engine...");
            return 1L;
        }
    }
}
