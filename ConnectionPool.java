import java.util.concurrent.Semaphore;

public class ConnectionPool {

    Semaphore semaphore;

    public ConnectionPool(int maxConnections) {
        this.semaphore = new Semaphore(maxConnections);

    }

    public void connection() throws InterruptedException {
        semaphore.acquire();
        System.out.println(
                Thread.currentThread().getName() + " CONNECTED    | permits left: " + semaphore.availablePermits());
        Thread.sleep(100);
    }

    public void release() {
        semaphore.release();
        System.out.println(
                Thread.currentThread().getName() + " disconnected | permits left: " + semaphore.availablePermits());
    }

    public static void main(String[] args) {
        ConnectionPool pool = new ConnectionPool(3);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    pool.connection();
                    pool.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

}
