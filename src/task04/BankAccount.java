package task04;

public class BankAccount {

    private final Long id;
    private long balanceGr;                 // saldo w GROSZACH

    public BankAccount(Long id, long initialPln) {
        this.id = id;
        this.balanceGr = initialPln * 100;
    }

    public synchronized void deposit(long amountGr) {
        if (amountGr <= 0) {
            throw new IllegalArgumentException("Kwota wpłaty musi być dodatnia: " + amountGr);
        }

        this.balanceGr += amountGr;
    }

    public synchronized void withdraw(long amountGr) {
        if (amountGr <= 0) {
            throw new IllegalArgumentException("Kwota wypłaty musi być dodatnia: " + amountGr);
        }

        if (amountGr > balanceGr) {
            throw new InsufficientFundsException(id, balanceGr, amountGr);
        }

        balanceGr -= amountGr;
    }

    public synchronized long getBalanceGr() {
        return balanceGr;
    }

    public Long getId() {
        return id;                          // pole final – synchronizacja niepotrzebna
    }
}