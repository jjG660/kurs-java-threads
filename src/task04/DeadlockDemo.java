package task04;

public class DeadlockDemo {

    public static void main(String[] args) throws InterruptedException {
        BankAccount a = new BankAccount(1L, 1000);
        BankAccount b = new BankAccount(2L, 1000);

        Thread t1 = new Thread(() -> transferDeadlocking(a, b, 100), "przelew-1-do-2");
        Thread t2 = new Thread(() -> transferDeadlocking(b, a, 100), "przelew-2-do-1");

        System.out.println("PID " + ProcessHandle.current().pid());

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    private static void transferDeadlocking(BankAccount from, BankAccount to, long amountGr) {
        synchronized (from) {
            sleep(50);
            synchronized (to) {
                from.withdraw(amountGr);
                to.deposit(amountGr);
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
