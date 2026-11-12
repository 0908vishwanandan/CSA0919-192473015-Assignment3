public class TokenCounter {

    private final int[] buffer = new int[5];

    private int count = 0;
    private int in = 0;
    private int out = 0;

    private int minObserved = 0;
    private int maxObserved = 0;

    public synchronized void put(int token)
            throws InterruptedException {

        // Wait while the counter is full
        while (count == buffer.length) {
            wait();
        }

        // Add token
        buffer[in] = token;
        in = (in + 1) % buffer.length;

        count++;

        updateStats();

        // Wake waiting threads
        notifyAll();
    }

    public synchronized int take()
            throws InterruptedException {

        // Wait while the counter is empty
        while (count == 0) {
            wait();
        }

        // Remove token
        int token = buffer[out];

        out = (out + 1) % buffer.length;

        count--;

        updateStats();

        // Wake waiting threads
        notifyAll();

        return token;
    }

    private void updateStats() {

        if (count < minObserved) {
            minObserved = count;
        }

        if (count > maxObserved) {
            maxObserved = count;
        }
    }

    public synchronized String getBufferRange() {

        return minObserved + " to " + maxObserved;
    }

    public static void main(String[] args)
            throws InterruptedException {

        TokenCounter counter = new TokenCounter();

        int[] collected = new int[2];

        // Producer thread
        Thread cook = new Thread(() -> {

            try {
                for (int t = 1; t <= 20; t++) {
                    counter.put(t);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Two consumer threads
        Thread[] students = new Thread[2];

        for (int s = 0; s < 2; s++) {

            final int id = s;

            students[s] = new Thread(() -> {

                try {
                    for (int k = 0; k < 10; k++) {

                        collected[id] += counter.take();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        cook.start();

        for (Thread t : students) {
            t.start();
        }

        cook.join();

        for (Thread t : students) {
            t.join();
        }

        System.out.println(
            "Sum of tokens collected: "
            + (collected[0] + collected[1])
            + " (expected 210)"
        );

        System.out.println(
            "Observed buffer range: " + counter.getBufferRange()
        );
    }
}
