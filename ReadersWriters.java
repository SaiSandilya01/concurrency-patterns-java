public class ReadersWriters {

    static class SharedResource {
        private int data = 0;
        private int activeReaders = 0;
        private boolean writerActive = false;

        public synchronized void startRead() throws InterruptedException {
            while (writerActive) {
                wait();
            }
            activeReaders++;

        }

        public synchronized void endRead() {
            activeReaders--; // I'm done reading
            if (activeReaders == 0) { // last reader — wake up any waiting writer
                notifyAll();
            }
        }

        public synchronized void startWrite() throws InterruptedException {
            while (activeReaders > 0 || writerActive) { // wait while readers active OR another writer writing
                wait();
            }
            writerActive = true;
        }

        public synchronized void endWrite() {
            writerActive = false;
            notifyAll();

        }
    }

    public static void main(String[] args) throws InterruptedException {
        SharedResource resource = new SharedResource();

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    resource.startRead();
                    System.out.println(Thread.currentThread().getName() + " reading: " + resource.data);
                    Thread.sleep(50);
                    resource.endRead();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Reader-" + i).start();
        }

        new Thread(() -> {
            try {
                resource.startWrite();
                resource.data = 42;
                System.out.println("Writer wrote: " + resource.data);
                resource.endWrite();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-1").start();
    }
}
