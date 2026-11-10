package task02;

import java.util.Random;

public class ParallelSum {

    private static final int SIZE = 1_000_000;
    private static final int PARTS = 4;

    /**
     * Zadanie liczące sumę fragmentu [from, to) i zapisujące ją pod swój indeks.
     */
    static class PartialSumTask implements Runnable {
        private final int[] data;
        private final int from;             // włącznie
        private final int to;               // wyłącznie
        private final long[] results;       // WSPÓLNA tablica wyników
        private final int index;            // MÓJ i tylko mój indeks w results

        public PartialSumTask(int[] data, int from, int to, long[] results, int index) {
            this.data = data;
            this.from = from;
            this.to = to;
            this.results = results;
            this.index = index;
        }

        @Override
        public void run() {
            long sum = 0;
            for (int i = from; i < to; i++) {
                sum += data[i];
            }
            results[index] = sum;
        }
    }

    static long sequentialSum(int[] data) {
        long sum = 0;
        for (int d : data) {
            sum += d;
        }
        return sum;
    }

    public static void main(String[] args) throws InterruptedException {
        long[] results = new long[PARTS];
        int[] data = new int[SIZE];

        Random random = new Random();
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextInt();
        }

        Thread[] threads = new Thread[PARTS];

        int batch = data.length / PARTS;
        int from;
        int to;

        long t0 = System.nanoTime();

        for (int p = 0; p < PARTS; p++) {
            from = p * batch;
            to = (p + 1) * batch;
            if (p + 1 == PARTS) {
                to = data.length;
            }

            PartialSumTask task = new PartialSumTask(data, from, to, results, p);
            threads[p] = new Thread(task, "Thread: " + p);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long parallelSum = 0;
        for (long r : results) {
            parallelSum += r;
        }
        System.out.println("Parallel sum time: " + (System.nanoTime() - t0) / 1_000_000);

        long t1 = System.nanoTime();
        long sequentialSum = sequentialSum(data);
        System.out.println("Sequential sum time: " + (System.nanoTime() - t1) / 1_000_000);


        System.out.println("Sequential: " + sequentialSum + " = " + "Parellel: " + parallelSum + " ? -> " + (sequentialSum == parallelSum));

    }
}

