import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumer {

    static class BoundedBuffer {
        Queue<Integer> queue = new LinkedList<>();
        int capacity;

        BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        synchronized void produce(int item) throws InterruptedException {
            while (queue.size() == capacity) {
                wait(); // buffer full — release lock and sleep
            }
            this.queue.offer(item);
            System.out.println("Produced: " + item);
            notify(); // notify consumer

            // TODO: add item, print it, notify consumer
        }

        synchronized void consume() throws InterruptedException {
            while (queue.isEmpty()) {
                wait(); // buffer empty — release lock and sleep
            }
            int item = this.queue.poll();
            System.out.println("Consumed: " + item);
            notify(); // notify producer
            // TODO: remove item, print it, notify producer
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer buffer = new BoundedBuffer(5);

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    buffer.produce(i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // TODO: create consumer thread that consumes 20 times
        // start both, join both
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    buffer.consume();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }
}
