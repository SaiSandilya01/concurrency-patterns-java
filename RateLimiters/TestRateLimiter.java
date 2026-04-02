import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TestRateLimiter {
    public static void main(String[] args) throws InterruptedException {
        RateLimiter limiter = new FixedWindowRateLimiter(5, 2000);
        String userId = "User_A";

        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalThreads);

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        System.out.println("Firing 100 concurrent requests for User_A...");

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (limiter.allowRequest(userId)) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        
        System.out.println("Expected Allowed: 5");
        System.out.println("Actual Allowed: " + allowed.get());
        System.out.println("Actual Rejected: " + rejected.get());

        if (allowed.get() == 5) {
            System.out.println("Success! The rate limiter is perfectly thread-safe.");
        } else {
            System.out.println("Race condition detected! Expected 5 but allowed " + allowed.get());
        }

        System.out.println("\nWaiting 2 seconds for the window to reset...");
        Thread.sleep(2100);

        System.out.println("Firing 1 request in the new window...");
        
        if (limiter.allowRequest(userId)) {
             System.out.println("Request allowed! Window reset successfully.");
        } else {
             System.out.println("Request rejected! Window did not reset.");
        }

        executor.shutdown();
    }
}
