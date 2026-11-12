import java.util.concurrent.CountDownLatch;

public class TransferDemo {

    static class Account {
        final int id;
        int balance;

        Account(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    // Unsafe method that deliberately creates deadlock
    static void unsafeTransfer(Account from, Account to,
                               CountDownLatch bothReady) {

        synchronized (from) {

            bothReady.countDown();

            try {
                // Both threads wait while holding their first account lock
                bothReady.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            synchronized (to) {
                if (from.balance >= 100) {
                    from.balance -= 100;
                    to.balance += 100;
                }
            }
        }
    }

    // Safe method using consistent lock ordering
    static void safeTransfer(Account from, Account to, int amount) {

        if (from == to) {
            return;
        }

        Account first;
        Account second;

        if (from.id < to.id) {
            first = from;
            second = to;
        } else {
            first = to;
            second = from;
        }

        synchronized (first) {
            synchronized (second) {

                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Demonstrate deadlock
        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        CountDownLatch bothReady = new CountDownLatch(2);

        Thread t1 = new Thread(
            () -> unsafeTransfer(a, b, bothReady)
        );

        Thread t2 = new Thread(
            () -> unsafeTransfer(b, a, bothReady)
        );

        // Daemon threads allow the demonstration program to finish
        // even though the two unsafe threads remain deadlocked.
        t1.setDaemon(true);
        t2.setDaemon(true);

        t1.start();
        t2.start();

        bothReady.await();

        System.out.println(
            "Deadlock setup: both threads hold opposite account locks."
        );

        // Demonstrate safe transfers using separate accounts
        Account c = new Account(3, 1000);
        Account d = new Account(4, 1000);

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                safeTransfer(c, d, 1);
            }
        });

        Thread t4 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                safeTransfer(d, c, 1);
            }
        });

        t3.start();
        t4.start();

        t3.join();
        t4.join();

        System.out.println("Safe transfer completed.");

        System.out.println("Account 3 balance: " + c.balance);
        System.out.println("Account 4 balance: " + d.balance);

        System.out.println(
            "Total balance: " + (c.balance + d.balance)
        );
    }
}
