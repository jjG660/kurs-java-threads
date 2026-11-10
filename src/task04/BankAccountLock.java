package task04;

import java.util.concurrent.locks.ReentrantLock;

public class BankAccountLock {

    private final Long id;
    private final ReentrantLock lock = new ReentrantLock();
    private long balanceGr;

    public BankAccountLock(Long id, long initialPln) {
        this.id = id;
        this.balanceGr = initialPln * 100;
    }

    /** Wywołujący MUSI trzymać lock tego konta. */
    void depositUnlocked(long amountGr)  {
        if (amountGr <= 0) {
            throw new IllegalArgumentException("Kwota wpłaty musi być dodatnia: " + amountGr);
        }

        balanceGr += amountGr;
    }

    /** Wywołujący MUSI trzymać lock tego konta. */
    void withdrawUnlocked(long amountGr) {
        if (amountGr <= 0) {
            throw new IllegalArgumentException("Kwota wypłaty musi być dodatnia: " + amountGr);
        }

        if (amountGr > balanceGr) {
            throw new InsufficientFundsException(id, balanceGr, amountGr);
        }

        balanceGr -= amountGr;
    }

    ReentrantLock lock() {
        return lock;
    }

    long balanceGr() {
        lock.lock();
        try {
            return balanceGr;
        } finally {
            lock.unlock();
        }
    }
}
