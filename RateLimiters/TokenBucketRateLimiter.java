import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final double tokensPerSecond;
    private final ConcurrentHashMap<String, UserBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long capacity, double tokensPerSecond) {
        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;
    }

    @Override
    public boolean allowRequest(String userId) {
        return buckets.computeIfAbsent(userId, k -> new UserBucket(capacity)).tryConsume();
    }

    class UserBucket {
        private double currentTokens;
        private long lastRefillTimestamp;

        public UserBucket(long capacity) {
            this.currentTokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            
            double tokensToAdd = (now - lastRefillTimestamp) * (tokensPerSecond / 1000.0);
            currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
            lastRefillTimestamp = now;
            
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
