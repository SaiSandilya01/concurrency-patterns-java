package DistributedLock;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TestDistributedLock {
    public static void main(String[] args) throws InterruptedException {
        // Spin up our fake "Redis" server
        RedisDistributedLock.Jedis redisServer = new RedisDistributedLock.Jedis();
        
        System.out.println("🚀 100 App Servers are waking up to run the Midnight Billing Cron Job...");

        int totalServers = 100;
        ExecutorService executor = Executors.newFixedThreadPool(totalServers);
        CountDownLatch stampedeLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalServers);
        
        AtomicInteger successfulBillingRuns = new AtomicInteger(0);

        for (int i = 0; i < totalServers; i++) {
            executor.submit(() -> {
                // Every server generates its own unique ID so it can protect its own locks!
                String serverId = UUID.randomUUID().toString();
                RedisDistributedLock distributedLock = new RedisDistributedLock(redisServer, "MIDNIGHT_BILLING", 10000);

                try {
                    stampedeLatch.await(); // Wait at the starting line...
                    
                    // Attempt to become the single Leader across all 100 nodes!
                    if (distributedLock.tryAcquireLock(serverId)) {
                        System.out.println("✅ Lock Acquired by Server: " + serverId + " - Executing Billing!");
                        successfulBillingRuns.incrementAndGet();
                        
                        // Release the lock when done so tomorrow's job can run.
                        distributedLock.releaseLock(serverId); 
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        stampedeLatch.countDown(); // Go!
        completionLatch.await();
        
        System.out.println("\n🎉 Final Midnight Billing Execution Count: " + successfulBillingRuns.get());
        if (successfulBillingRuns.get() == 1) {
            System.out.println("🏆 The architecture is PERFECT. Exactly one server handled the job.");
        } else {
            System.out.println("❌ DISASTER! Users were billed " + successfulBillingRuns.get() + " times!");
        }
        
        executor.shutdown();
    }
}
