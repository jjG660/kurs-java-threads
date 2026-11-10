# Wnioski - zadanie 4

## 1. Struktura danych na konta

Wybrałem zwykłą `HashMap<Long, BankAccount>`. `transfer` dostaje `fromId` i `toId`, więc
potrzebuję dostępu po kluczu - `get`.
Konta zakłada jeden wątek przed startem puli, a potem
inne wątki wykonują tylko odczyty, tysiąc razy. Do struktury, do której nikt już nie pisze, nie
potrzeba żadnej ochrony -bezpieczeństwo bierze się stąd, że jest kompletna, zanim wystartuje
pierwszy wątek.

Co odrzuciłem:

* `ConcurrentHashMap` - poprawna, ale rozwiązuje problem, którego tu nie ma. Płaciłbym za
  współbieżny zapis, który nigdy nie następuje.
* `Collections.synchronizedMap` - najgorszy wybór z trzech: serializuje wszystkie odczyty
  na jednym monitorze, czyli wstawia wąskie gardło dokładnie tam, gdzie ruch jest największy.
* `List`/`ArrayList` z wyszukiwaniem po `getId()` - O(n) na każdy przelew.


## 2. Cecha wyznaczająca globalną kolejność blokowania

`id` konta - pole `final`, ustawiane w konstruktorze i nigdy niezmieniane. To jest jedyny
wymóg: kolejność musi być taka sama dla każdego wątku i stała w czasie. Gdybym porządkował
po saldzie, kolejność zmieniałaby się przy każdym przelewie i cykl wróciłby.


## 3. Dlaczego wariant z `tryLock` nie potrzebuje tej kolejności

Bo stosuje odwrotną strategię: zamiast uczynić cykl niemożliwym, wychodzi z niego.
`BankLock.transfer` zajmuje zamek konta źródłowego z limitem 50 ms, a jeśli drugiego zamka nie uda
się zająć w kolejnych 50 ms - zwalnia pierwszy, śpi 1 ms i próbuje od nowa.
Oddanie pierwszego zamka to krok, który rozrywa cykl: warunek „trzymam swój i czekam na twój"
przestaje obowiązywać, więc zakleszczenie nie ma z czego powstać, nawet przy losowej kolejności.


## 4. Dlaczego `AtomicReference` nie wystarcza do przelewu między dwoma kontami

Bo CAS działa na jednej komórce pamięci. `compareAndSet` potrafi niepodzielnie podmienić
migawkę jednego konta i nie istnieje operacja procesora, która zrobiłaby to samo dla dwóch
niezależnych referencji naraz. „`tryWithdraw` na A, potem `deposit` na B" to więc dwie operacje,
z których każda z osobna jest atomowa, ale razem nie tworzą jednej- i nie ma zamka, który by je
spiął, bo cały sens tego wariantu polega na jego braku.

## 5. Co pokazał zrzut wątków przy celowo wprowadzonym zakleszczeniu

`DeadlockDemo` bierze zamki w kolejności argumentów, bez porządkowania po id, i wstawia 50 ms
pauzy między pierwszym a drugim monitorem. Dwa wątki przelewające w przeciwne strony zakleszczają
się na pierwszej próbie. Program nie rzuca wyjątku i nic nie wypisuje do logu - po prostu stoi,
a `join()` w `main` nigdy nie wraca.

`jstack <pid>` (to samo, co Thread.print w IntelliJ) rozpoznał problem sam i nazwał go wprost:

```
Found one Java-level deadlock:
=============================
"przelew-1-do-2":
  waiting to lock monitor 0x0000017570252ae0 (object 0x000000062d020f70, a task04.BankAccount),
  which is held by "przelew-2-do-1"
"przelew-2-do-1":
  waiting to lock monitor 0x0000017570252d50 (object 0x000000062d020eb8, a task04.BankAccount),
  which is held by "przelew-1-do-2"

Java stack information for the threads listed above:
===================================================
"przelew-1-do-2":
	at task04.DeadlockDemo.transferDeadlocking(DeadlockDemo.java:35)
	- waiting to lock <0x000000062d020f70> (a task04.BankAccount)
	- locked <0x000000062d020eb8> (a task04.BankAccount)
"przelew-2-do-1":
	at task04.DeadlockDemo.transferDeadlocking(DeadlockDemo.java:35)
	- waiting to lock <0x000000062d020eb8> (a task04.BankAccount)
	- locked <0x000000062d020f70> (a task04.BankAccount)

Found 1 deadlock.
```
