package task04;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class BankLock {

    private final Map<Long, BankAccountLock> accounts = new HashMap<>();

    private static final long DEADLINE_SECONDS = 1;
    private static final long LOCK_TIMEOUT_MS = 50;

    public void openAccount(long id, long initialPln) {
        accounts.put(id, new BankAccountLock(id, initialPln));
    }

    public boolean transfer(long fromId, long toId, long amountGr) {
        if (fromId == toId) {
            throw new IllegalArgumentException("Przelew na to samo konto: " + fromId);
        }

        BankAccountLock from = accounts.get(fromId);
        BankAccountLock to = accounts.get(toId);

        ReentrantLock fromLock = from.lock();
        ReentrantLock toLock = to.lock();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS);

        try {
            while (System.nanoTime() < deadline) {
                long remaining = deadline - System.nanoTime();

                long waitTime = Math.min(
                        remaining,
                        TimeUnit.MILLISECONDS.toNanos(LOCK_TIMEOUT_MS)
                );

                if (fromLock.tryLock(waitTime, TimeUnit.NANOSECONDS)) {
                    try {
                        remaining = deadline - System.nanoTime();

                        if (remaining <= 0) {
                            return false;
                        }

                        waitTime = Math.min(
                                remaining,
                                TimeUnit.MILLISECONDS.toNanos(LOCK_TIMEOUT_MS)
                        );

                        if (toLock.tryLock(waitTime, TimeUnit.NANOSECONDS)) {

                            try {
                                from.withdrawUnlocked(amountGr);
                                to.depositUnlocked(amountGr);

                                return true;

                            } finally {
                                toLock.unlock();
                            }
                        }

                    } finally {
                        fromLock.unlock();
                    }
                }

                Thread.sleep(1);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return false;
    }

    public long balanceGr(long id) {
        return accounts.get(id).balanceGr();
    }

    public long totalBalanceGr() {
        return accounts.values().stream()
                .mapToLong(BankAccountLock::balanceGr)
                .sum();
    }
}
