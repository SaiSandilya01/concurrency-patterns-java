package InterviewStyleQuestions.ABCPattern;

public class PrintABC {

    static final Object lock = new Object();
    static int cycles = 0;
    static final int LIMIT = 100;

    public enum State {
        A, B, C
    }

    static State state = State.A;

    public static void printA() {
        synchronized (lock) {
            while (cycles < LIMIT) {
                while (cycles < LIMIT && state != State.A) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (cycles < LIMIT) {
                    System.out.println("A");
                    state = State.B;
                    lock.notifyAll();
                }
            }

            lock.notifyAll();
        }
    }

    public static void printB() {
        synchronized (lock) {
            while (cycles < LIMIT) {
                while (cycles < LIMIT && state != State.B) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (cycles < LIMIT) {
                    System.out.println("B");
                    state = State.C;
                    lock.notifyAll();
                }
            }

            lock.notifyAll();
        }
    }

    public static void printC() {
        synchronized (lock) {
            while (cycles < LIMIT) {
                while (cycles < LIMIT && state != State.C) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (cycles < LIMIT) {
                    System.out.println("C");
                    state = State.A;
                    cycles++;
                    lock.notifyAll();
                }
            }

            lock.notifyAll();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread a = new Thread(() -> printA());
        Thread b = new Thread(() -> printB());
        Thread c = new Thread(() -> printC());

        a.start();
        b.start();
        c.start();

        a.join();
        b.join();
        c.join();

        System.out.println("Execution done");
    }
}