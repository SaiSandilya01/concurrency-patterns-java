import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TestTokenBucket {
    public static void main(String[] args) throws InterruptedException {
        RateLimiter limiter = new TokenBucketRateLimiter(5, 2.0);
        String userId = "User_A";

        System.out.println("Firing a burst of 10 requests immediately...");
        int allowedBurst = 0;
        for (int i = 0; i < 10; i++) {
            if (limiter.allowRequest(userId)) allowedBurst++;
        }
        System.out.println("Initially allowed (should be 5, utilizing the bucket's burst capacity): " + allowedBurst);
        
        System.out.println("\nWaiting exactly 505ms to test sustained pacing...");
        Thread.sleep(505);

        System.out.println("Firing another 5 requests...");
        int allowedSustained = 0;
        for (int i = 0; i < 5; i++) {
            if (limiter.allowRequest(userId)) allowedSustained++;
        }
        System.out.println("Sustained allowed (should be exactly 1): " + allowedSustained);

        System.out.println("\nWaiting 2500ms (bucket should fully refill)...");
        Thread.sleep(2550);
        
        System.out.println("Firing 100 concurrent requests at the EXACT same millisecond to test thread safety...");
        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalThreads);
        AtomicInteger finalAllowed = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (limiter.allowRequest(userId)) finalAllowed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally { 
                    endLatch.countDown(); 
                }
            });
        }
        
        startLatch.countDown();
        endLatch.await();
        
        System.out.println("Concurrent requests allowed (should be exactly 5): " + finalAllowed.get());
        if (finalAllowed.get() == 5) {
             System.out.println("Perfection. The Token Bucket algorithm works!");
        } else {
             System.out.println("Failed. A race condition let " + finalAllowed.get() + " requests through instead of 5.");
        }
        
        executor.shutdown();
    }
}
