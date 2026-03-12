public class DiningPhilosophers {

    static Object[] forks = new Object[5]; // 5 forks as lock objects

    static void philosopher(int id) throws InterruptedException {
        int leftFork = id;
        int rightFork = (id + 1) % 5;

        while (true) {

            System.out.println("Philosopher " + id + " is thinking");
            Thread.sleep(100);

            int firstFork = Math.min(leftFork, rightFork);
            int secondFork = Math.max(leftFork, rightFork);

            synchronized (forks[firstFork]) {
                Thread.sleep(10);
                synchronized (forks[secondFork]) {
                    System.out.println("Philosopher " + id + " is eating");
                    Thread.sleep(100);
                }
            }

        }
    }

    public static void main(String[] args) {
        // Initialize the lock objects (forks)
        for (int i = 0; i < 5; i++) {
            forks[i] = new Object();
        }

        // Start 5 philosopher threads
        for (int i = 0; i < 5; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    philosopher(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Philosopher-" + i).start();
        }
    }
}
