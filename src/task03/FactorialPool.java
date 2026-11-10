package task03;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class FactorialPool {

    private static final int POOL_SIZE = 4;
    private static final int MAX_N = 20;

    /**
     * Liczy n! i zwraca wynik; w środku losowy sleep 100–500 ms.
     */
    static BigInteger factorial(int n) throws InterruptedException {
        System.out.println(Thread.currentThread().getName());

        BigInteger result = BigInteger.ONE;
        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 501));
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        List<Future<BigInteger>> futures = new ArrayList<>();

        for (int n = 1; n <= MAX_N; n++) {
            int number = n;
            futures.add(pool.submit(() -> factorial(number)));
        }

        pool.shutdown();

        for (int n = 0; n < futures.size(); n++) {
            System.out.println((n + 1) + "-> " + futures.get(n).get());
        }

        System.out.println("Time: " + (System.nanoTime() - t0) / 1_000_000);
    }
}
