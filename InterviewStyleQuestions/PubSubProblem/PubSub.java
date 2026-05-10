package InterviewStyleQuestions.PubSubProblem;

import java.util.LinkedList;
import java.util.Queue;

public class PubSub {

    private final Queue<Integer> queue;
    private final int capacity;
    private final Object lock = new Object();

    public PubSub(int capacity) {
        this.queue = new LinkedList<>();
        this.capacity = capacity;
    }

    public void addElement(int item) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == capacity) {
                System.out.println("Queue full. Producer waiting...");
                lock.wait();
            }

            queue.offer(item);
            System.out.println("Produced: " + item + " | Queue: " + queue);

            lock.notifyAll();
        }
    }

    public int consumeElement() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                System.out.println("Queue empty. Consumer waiting...");
                lock.wait();
            }

            int item = queue.poll();
            System.out.println("Consumed: " + item + " | Queue: " + queue);

            lock.notifyAll();

            return item;
        }
    }

    public static void main(String[] args) {
        PubSub pubSub = new PubSub(5);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    pubSub.addElement(i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer-Thread");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 15; i++) {
                    pubSub.consumeElement();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-Thread");

        producer.start();
        consumer.start();
    }
}