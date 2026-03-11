import java.util.LinkedList;
import java.util.Queue;

public class ThreadPool {

    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final Thread[] workers;
    private boolean isShutdown = false;

    public ThreadPool(int numThreads) {
        workers = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Thread(() -> {
                while (true) {
                    Runnable task = null;
                    synchronized (this) {
                        while (taskQueue.isEmpty() && !isShutdown) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        if (isShutdown && taskQueue.isEmpty()) {
                            return;
                        }
                        task = taskQueue.poll();
                        notifyAll();
                    }
                    if (task != null) {
                        task.run();
                    }
                }
            });
            workers[i].start();
        }
    }

    public synchronized void submit(Runnable task) throws InterruptedException {
        while (taskQueue.size() >= workers.length) {
            wait();
        }
        taskQueue.offer(task);
        notify();

    }

    public synchronized void shutdown() {
        isShutdown = true;
        notifyAll();
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPool pool = new ThreadPool(3);

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("Task " + taskId + " running on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(2000);
        pool.shutdown();
    }
}
