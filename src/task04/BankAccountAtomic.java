package task04;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Konto bez zamków: saldo nie jest polem do modyfikacji, tylko siedzi w niemutowalnej
 * migawce podmienianej w całości przez CAS.
 *
 * Wniosek: wzorzec jest elegancki i nie blokuje żadnego wątku, ale operacja warunkowa
 * („wypłać, jeśli stać") wymaga ręcznej pętli CAS, bo updateAndGet zawsze coś ustawia
 * i nie ma jak przez niego powiedzieć „nie zrobiłem nic". Jeden blok synchronized albo
 * ReentrantLock czyta się tu po prostu naturalniej – nie każdy problem jest problemem dla CAS.
 *
 * Czego ten wariant nie umie: niepodzielnego przelewu między DWOMA kontami. CAS działa na
 * jednej komórce pamięci, więc „tryWithdraw na A, potem deposit na B" to dwie niezależne
 * operacje atomowe, a nie jedna – nie ma zamka, który spiąłby je w całość. Obserwator, który
 * zsumuje salda dokładnie w szczelinie między nimi, zobaczy sumę mniejszą o kwotę przelewu:
 * pieniądze zniknęły już z A, a na B jeszcze się nie pojawiły. Co gorsza, gdyby drugi krok
 * zawiódł, nie ma czym wycofać pierwszego.
 */
public class BankAccountAtomic {

    /**
     * Niemutowalna migawka stanu konta.
     */
    private record Snapshot(long balanceGr, long version) {
    }

    private final AtomicReference<Snapshot> state;

    public BankAccountAtomic(long initialPln) {
        this.state = new AtomicReference<>(new Snapshot(initialPln * 100, 0));
    }

    public void deposit(long amountGr) {
        state.updateAndGet(current ->
                new Snapshot(
                        current.balanceGr() + amountGr,
                        current.version() + 1
                )
        );
    }

    /**
     * @return true gdy wypłacono, false gdy brak środków.
     */
    public boolean tryWithdraw(long amountGr) {
        while (true) {
            Snapshot current = state.get();

            if (current.balanceGr() < amountGr) {
                return false;
            }

            Snapshot updated = new Snapshot(
                    current.balanceGr() - amountGr,
                    current.version() + 1
            );

            if (state.compareAndSet(current, updated)) {
                return true;
            }
        }
    }

    public long balanceGr() {
        return state.get().balanceGr();
    }

    public long version() {
        return state.get().version();
    }
}
