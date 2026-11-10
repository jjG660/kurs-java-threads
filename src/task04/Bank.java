package task04;

import java.util.HashMap;
import java.util.Map;

/**
 * Konta trzyma zwykła HashMap: zakłada je jeden wątek przed startem symulacji, a potem
 * mapa jest wyłącznie czytana, więc nie ma zapisu, przed którym trzeba by
 * ją chronić. ConcurrentHashMap dołożyłaby narzut bez żadnej korzyści, a
 * Collections.synchronizedMap serializowałaby wszystkie odczyty na jednym monitorze.
 * Warunkiem poprawności jest to, że openAccount nie jest wołane po starcie wątków.
 */
public class Bank {

    private final Map<Long, BankAccount> accounts;

    public Bank() {
        this.accounts = new HashMap<>();
    }

    public void openAccount(long id, long initialPln) {
        accounts.put(id, new BankAccount(id, initialPln));
    }

    /** Atomowy przelew: albo obie strony, albo żadna. NIE może się zakleszczyć. */
    public void transfer(long fromId, long toId, long amountGr) {
        if (fromId == toId) {
            throw new IllegalArgumentException("Przelew na to samo konto: " + fromId);
        }

        BankAccount from = accounts.get(fromId);
        BankAccount to = accounts.get(toId);

        BankAccount first = fromId > toId ? from : to;
        BankAccount second = fromId > toId ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.withdraw(amountGr);
                to.deposit(amountGr);
            }
        }
    }

    public long balanceGr(long id) {
        return accounts.get(id).getBalanceGr();
    }

    /** Suma sald wszystkich kont w groszach.
     *
     * Metoda powinna być wywoływana po zakończeniu wszystkich transakcji.
     * Wywołana podczas równoległych przelewów może zwrócić niespójną sumę,
     * ponieważ poszczególne konta są odczytywane niezależnie i między
     * odczytami może nastąpić zmiana salda.
     *
     * Sama synchronizacja tej metody nie rozwiązałaby problemu, ponieważ
     * przelewy synchronizują się na obiektach kont, a nie na obiekcie Bank.
     * Uzyskanie spójnej migawki wymagałoby wspólnej blokady obejmującej
     * wszystkie konta, używanej zarówno przez przelewy, jak i odczyt sumy,
     * albo mechanizmu tworzenia spójnej migawki całego banku.
     * */
    public long totalBalanceGr() {
        return accounts.values().stream()
                .mapToLong(BankAccount::getBalanceGr)
                .sum();
    }
}