package InterviewStyleQuestions.DeadlockImplementation;

public class DeadlockResolution {
    public static void bankTransferAB(Bank A, Bank B) throws InterruptedException {

        synchronized (A) {
            Thread.sleep(2000);
            synchronized (B) {
                int temp = A.getBalance();
                B.setBalance(B.getBalance() + temp);
                A.setBalance(0);

            }
        }

    }

    public static void bankTransferBA(Bank B, Bank A) throws InterruptedException {

        synchronized (A) {
            Thread.sleep(2000);
            synchronized (B) {
                int temp = B.getBalance();
                A.setBalance(A.getBalance() + temp);
                B.setBalance(0);

            }
        }

    }

    public static void main(String[] args) {

        Bank A = new Bank(100);
        Bank B = new Bank(150);

        Thread a = new Thread(() -> {
            try {
                DeadlockResolution.bankTransferAB(A, B);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        Thread b = new Thread(() -> {
            try {
                DeadlockResolution.bankTransferBA(B, A);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        a.start();
        b.start();

        try {
            a.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            b.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("A balance" + A.getBalance());

        System.out.println("B balance" + B.getBalance());

        System.out.println("Execution done");

    }

}
