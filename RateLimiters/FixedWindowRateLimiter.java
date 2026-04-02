import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter implements RateLimiter {

    private final int limit;
    private final long windowSizeMs;
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private volatile long windowStartTime;

    public FixedWindowRateLimiter(int limit, long windowSizeMs) {
        this.limit = limit;
        this.windowSizeMs = windowSizeMs;
        this.windowStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        
        if (now - windowStartTime >= windowSizeMs) {
            synchronized (this) {
                if (now - windowStartTime >= windowSizeMs) {
                    requestCounts.clear();
                    this.windowStartTime = windowSizeMs * (now / windowSizeMs);
                }
            }
        }

        AtomicInteger counter = requestCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= limit;
    }
}
