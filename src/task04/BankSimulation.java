package task04;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongUnaryOperator;


public class BankSimulation {

    private static final int ACCOUNTS = 10;
    private static final long INITIAL_PLN = 1000;
    private static final int TRANSFERS = 1000;
    private static final int POOL_SIZE = 8;
    private static final int MIN_PLN = 100;
    private static final int MAX_PLN = 500;
    private static final long SEED = 42;

    private static final long EXPECTED_TOTAL_GR = ACCOUNTS * INITIAL_PLN * 100L;

    private record Transfer(long fromId, long toId, long amountGr) {}

    public static void main(String[] args) {
        List<Transfer> transfers = randomTransfers();

        runSynchronized(transfers);
        runReentrantLock(transfers);
        runAtomic(transfers);
    }

    private static List<Transfer> randomTransfers() {
        Random random = new Random(SEED);
        List<Transfer> transfers = new ArrayList<>(TRANSFERS);

        for (int i = 0; i < TRANSFERS; i++) {
            long fromId = random.nextInt(ACCOUNTS);
            long toId = random.nextInt(ACCOUNTS);
            while (toId == fromId) {
                toId = random.nextInt(ACCOUNTS);
            }

            long amountGr = random.nextInt(MIN_PLN, MAX_PLN + 1) * 100L;
            transfers.add(new Transfer(fromId, toId, amountGr));
        }

        return transfers;
    }

    private static void runSynchronized(List<Transfer> transfers) {
        Bank bank = new Bank();
        for (long id = 0; id < ACCOUNTS; id++) {
            bank.openAccount(id, INITIAL_PLN);
        }

        AtomicInteger rejected = new AtomicInteger();

        runAll(transfers, t -> {
            try {
                bank.transfer(t.fromId(), t.toId(), t.amountGr());
            } catch (InsufficientFundsException e) {
                rejected.incrementAndGet();
            }
        });

        System.out.println("=== 1. synchronized (Bank + BankAccount) ===");
        printBalances(bank::balanceGr);
        System.out.println("  odrzucone (brak środków): " + rejected.get() + " / " + TRANSFERS);
        printVerdict(bank.totalBalanceGr());
    }

    private static void runReentrantLock(List<Transfer> transfers) {
        BankLock bank = new BankLock();
        for (long id = 0; id < ACCOUNTS; id++) {
            bank.openAccount(id, INITIAL_PLN);
        }

        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger givenUp = new AtomicInteger();

        runAll(transfers, t -> {
            try {
                if (!bank.transfer(t.fromId(), t.toId(), t.amountGr())) {
                    givenUp.incrementAndGet();
                }
            } catch (InsufficientFundsException e) {
                rejected.incrementAndGet();
            }
        });

        System.out.println("=== 2. ReentrantLock + tryLock (BankLock + BankAccountLock) ===");
        printBalances(bank::balanceGr);
        System.out.println("  odrzucone (brak środków):  " + rejected.get() + " / " + TRANSFERS);
        System.out.println("  rezygnacje (termin minął): " + givenUp.get() + " / " + TRANSFERS);
        printVerdict(bank.totalBalanceGr());
    }

    private static void runAtomic(List<Transfer> transfers) {
        Map<Long, BankAccountAtomic> accounts = new HashMap<>();
        for (long id = 0; id < ACCOUNTS; id++) {
            accounts.put(id, new BankAccountAtomic(INITIAL_PLN));
        }

        AtomicInteger rejected = new AtomicInteger();

        runAll(transfers, t -> {
            if (!transferAtomic(accounts, t)) {
                rejected.incrementAndGet();
            }
        });

        System.out.println("=== 3. AtomicReference (BankAccountAtomic) ===");
        printBalances(id -> accounts.get(id).balanceGr());
        System.out.println("  odrzucone (brak środków): " + rejected.get() + " / " + TRANSFERS);

        long versions = 0;
        long totalGr = 0;
        for (long id = 0; id < ACCOUNTS; id++) {
            versions += accounts.get(id).version();
            totalGr += accounts.get(id).balanceGr();
        }

        long succeeded = TRANSFERS - rejected.get();
        System.out.println("  suma version po kontach:  " + versions
                + " (operacji na kontach: 2 x " + succeeded + " udanych przelewów = "
                + 2 * succeeded + ")");

        printVerdict(totalGr);
    }


    private static boolean transferAtomic(Map<Long, BankAccountAtomic> accounts, Transfer t) {
        BankAccountAtomic from = accounts.get(t.fromId());
        BankAccountAtomic to = accounts.get(t.toId());

        if (!from.tryWithdraw(t.amountGr())) {
            return false;
        }

        to.deposit(t.amountGr());
        return true;
    }


    private static void runAll(List<Transfer> transfers, Consumer<Transfer> action) {
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);

        try {
            CompletableFuture<?>[] all = new CompletableFuture<?>[transfers.size()];

            for (int i = 0; i < transfers.size(); i++) {
                final Transfer transfer = transfers.get(i);
                all[i] = CompletableFuture.runAsync(() -> action.accept(transfer), pool);
            }

            CompletableFuture.allOf(all).join();
        } finally {
            pool.shutdown();
        }
    }

    private static void printBalances(LongUnaryOperator balanceOfAccount) {
        for (long id = 0; id < ACCOUNTS; id++) {
            System.out.println("  konto " + id + ": " + pln(balanceOfAccount.applyAsLong(id)));
        }
    }

    private static void printVerdict(long totalGr) {
        System.out.println("  SUMA SALD: " + pln(totalGr) + " (oczekiwane " + pln(EXPECTED_TOTAL_GR) + ")");

        if (totalGr != EXPECTED_TOTAL_GR) {
            throw new AssertionError("NIEZMIENNIK ZŁAMANY – gdzieś jest race condition!");
        }

        System.out.println("  WERDYKT: OK, niezmiennik zachowany");
        System.out.println();
    }

    private static String pln(long gr) {
        return gr / 100 + "," + String.format("%02d", gr % 100) + " PLN";
    }
}
