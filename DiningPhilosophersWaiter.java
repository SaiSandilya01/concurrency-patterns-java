public class DiningPhilosophersWaiter {

    // The Arbitrator / Waiter that controls who can eat
    static class Waiter {
        // Track the state of each fork (true = in use, false = free)
        private boolean[] forks = new boolean[5];

        public synchronized void pickUpForks(int philosopherId) throws InterruptedException {
            int leftFork = philosopherId;
            int rightFork = (philosopherId + 1) % 5;

            // Wait until BOTH forks are free
            while (forks[leftFork] || forks[rightFork]) {
                wait();
            }

            // Mark both forks as in use
            forks[leftFork] = true;
            forks[rightFork] = true;
        }

        public synchronized void putDownForks(int philosopherId) {
            int leftFork = philosopherId;
            int rightFork = (philosopherId + 1) % 5;

            // Mark both forks as free
            forks[leftFork] = false;
            forks[rightFork] = false;

            // Notify waiting philosophers that forks are available
            notifyAll();
        }
    }

    static void philosopher(int id, Waiter waiter) throws InterruptedException {
        while (true) {
            System.out.println("Philosopher " + id + " is thinking");
            Thread.sleep(100);

            waiter.pickUpForks(id);

            System.out.println("Philosopher " + id + " is eating");
            Thread.sleep(100);

            waiter.putDownForks(id);
        }
    }

    public static void main(String[] args) {
        Waiter waiter = new Waiter();

        for (int i = 0; i < 5; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    philosopher(id, waiter);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Philosopher-" + i).start();
        }
    }
}
