package task04;

/** Nieoznaczony (unchecked) – nie zmusza do try/catch w każdej lambdzie. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId, long balanceGr, long requestedGr) {
        super("Nie wystarczające środki na koncie: " + accountId + ". Aktuale saldo: " + balanceGr + ". Wniosek o wypłatę: " + requestedGr);
    }
}
