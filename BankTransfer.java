public class BankTransfer {

    static class BankAccount {
        int id;
        double balance;

        BankAccount(int id, double balance) {
            // TODO: initialize fields
            this.id = id;
            this.balance = balance;
        }

        void transfer(BankAccount other, double amount) {
            // VERSION 1 (broken):
            // lock THIS first, then OTHER, then do the transfer
            // TODO
            BankAccount first = this.id < other.id ? this : other;
            BankAccount second = this.id < other.id ? other : this;
            synchronized (first) {
                synchronized (second) {
                    this.balance -= amount;
                    other.balance += amount;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BankAccount accountA = new BankAccount(1, 1000);
        BankAccount accountB = new BankAccount(2, 1000);

        // Thread 1: A → B
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++)
                accountA.transfer(accountB, 1);
        });

        // Thread 2: B → A ← this is the opposite direction, causing circular wait
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++)
                accountB.transfer(accountA, 1);
            // TODO: transfer from B to A
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("A: " + accountA.balance + " B: " + accountB.balance);
        // Expected: A=1000, B=1000 (same net — just money moving back and forth)
    }
}
