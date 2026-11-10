package task01;

import java.util.concurrent.atomic.AtomicInteger;

public class CounterTask implements Runnable {

    static AtomicInteger started = new AtomicInteger(0);

    private final int number;

    public CounterTask(int number) {
        this.number = number;
    }

    @Override
    public void run() {
        System.out.println("Wątek:" + number + " start");
        for (int i = 1; i <= 10; i++) {
            try {
                Thread.sleep(100);
                System.out.println("Wątek " + number  +  ": " + i);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 1; i <= 100_000; i++) {
            started.incrementAndGet();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final int N = 5;

        Thread[] threads = new Thread[N];
        for (int i = 0; i < threads.length; i++) {
            CounterTask task = new CounterTask(i + 1);
            threads[i] = new Thread(task, "Thread-" + i + 1);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("Started: " + started.get());

        System.out.println("Wszystkie wątki zakończyły działanie.");
    }
}

