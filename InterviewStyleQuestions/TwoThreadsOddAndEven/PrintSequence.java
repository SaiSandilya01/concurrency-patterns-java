package InterviewStyleQuestions.TwoThreadsOddAndEven;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PrintSequence {

    static final Object lock = new Object();

    static final int max = 100;
    static int current = 0;

    public static void printOdd(int n) throws InterruptedException {
        synchronized (lock) {

            while (current <= max) {
                while (current % 2 == 0) {
                    lock.wait();
                }
                if (current <= max) {
                    System.out.println(current);
                    current++;
                    lock.notifyAll();
                }

            }

        }

    }

    public static void printEven(int n) throws InterruptedException {
        synchronized (lock) {

            while (current <= max) {
                while (current % 2 == 1) {
                    lock.wait();
                }

                if (current <= max) {
                    System.out.println(current);
                    current++;
                    lock.notifyAll();
                }

            }

        }
    }

    public static void main(String[] args) throws InterruptedException {

        Thread odd = new Thread(() -> {
            try {
                PrintSequence.printOdd(current);
            } catch (Exception e) {
                // TODO: handle exception
            }
        });
        Thread even = new Thread(() -> {
            try {
                PrintSequence.printEven(current);
            } catch (Exception e) {
                // TODO: handle exception
            }
        });
        odd.start();
        even.start();

        odd.join();
        even.join();

        System.out.println("Execution done");

    }

}
