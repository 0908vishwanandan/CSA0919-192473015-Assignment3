class ScannerStation extends Thread {

    int scripts;
    int stationNumber;

    ScannerStation(int stationNumber, int scripts) {
        this.stationNumber = stationNumber;
        this.scripts = scripts;
    }

    public void run() {

        try {
            System.out.println("Station " + stationNumber + " started");

            Thread.sleep(1000 + stationNumber * 500);

            System.out.println("Station " + stationNumber +
                    " scanned " + scripts + " scripts");

        } catch (InterruptedException e) {
            System.out.println("Station interrupted");
            Thread.currentThread().interrupt();
        }
    }
}

class RunnableStation implements Runnable {

    int scripts;
    int stationNumber;

    RunnableStation(int stationNumber, int scripts) {
        this.stationNumber = stationNumber;
        this.scripts = scripts;
    }

    public void run() {

        try {
            System.out.println("Station " + stationNumber + " started");

            Thread.sleep(1000 + stationNumber * 500);

            System.out.println("Station " + stationNumber +
                    " scanned " + scripts + " scripts");

        } catch (InterruptedException e) {
            System.out.println("Station interrupted");
            Thread.currentThread().interrupt();
        }
    }
}

public class Main {

    public static void main(String[] args) {

        int total = 0;

        // Method 1: Extending Thread class
        System.out.println("USING THREAD CLASS");

        ScannerStation t1 = new ScannerStation(1, 100);
        ScannerStation t2 = new ScannerStation(2, 150);
        ScannerStation t3 = new ScannerStation(3, 120);
        ScannerStation t4 = new ScannerStation(4, 130);

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        System.out.println("Station 1 alive: " + t1.isAlive());

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        total = 100 + 150 + 120 + 130;

        System.out.println("Total scripts scanned: " + total);

        // Method 2: Implementing Runnable interface
        System.out.println("\nUSING RUNNABLE INTERFACE");

        Thread r1 = new Thread(new RunnableStation(1, 100));
        Thread r2 = new Thread(new RunnableStation(2, 150));
        Thread r3 = new Thread(new RunnableStation(3, 120));
        Thread r4 = new Thread(new RunnableStation(4, 130));

        r1.start();
        r2.start();
        r3.start();
        r4.start();

        try {
            r1.join();
            r2.join();
            r3.join();
            r4.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Total scripts scanned: " + total);
    }
}
