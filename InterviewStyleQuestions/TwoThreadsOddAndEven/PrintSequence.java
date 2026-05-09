package InterviewStyleQuestions.TwoThreadsOddAndEven;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PrintSequence {

    static final Lock lock = new ReentrantLock();
    static final Condition condition = lock.newCondition();

    static final int max = 100;
    static int current = 1;

    public static void printOdd() {
        while (true) {
            lock.lock();
            try {
                while (current <= max && current % 2 == 0) {
                    condition.await();
                }

                if (current > max) {
                    condition.signalAll();
                    return;
                }

                System.out.println(current + " printed by odd");
                current++;
                condition.signalAll();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void printEven() {
        while (true) {
            lock.lock();
            try {
                while (current <= max && current % 2 == 1) {
                    condition.await();
                }

                if (current > max) {
                    condition.signalAll();
                    return;
                }

                System.out.println(current + " printed by even");
                current++;
                condition.signalAll();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        Thread odd = new Thread(PrintSequence::printOdd);
        Thread even = new Thread(PrintSequence::printEven);

        odd.start();
        even.start();

        odd.join();
        even.join();

        System.out.println("Execution done");
    }
}