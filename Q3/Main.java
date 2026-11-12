import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {

        Path folder = Paths.get("sales-input");
        Files.createDirectories(folder);

        Path store1 = folder.resolve("store1.txt");
        Path store2 = folder.resolve("store2.txt");
        Path store3 = folder.resolve("store3.txt");

        // Creating sample sales files
        Files.writeString(store1, "100\n200\n");
        Files.writeString(store2, "50\nCORRUPT\n70\n");
        Files.writeString(store3, "80\n120\n");

        List<Path> files = Arrays.asList(store1, store2, store3);

        // Manual closing using finally
        System.out.println("MANUAL TRY-CATCH-FINALLY VERSION");

        int total1 = generateReportWithFinally(files);

        System.out.println("Daily total: " + total1);
        System.out.println("Open readers: " +
                TrackedReader.getOpenCount());

        // Automatic closing using try-with-resources
        System.out.println("\nTRY-WITH-RESOURCES VERSION");

        int total2 = generateReportWithTWR(files);

        System.out.println("Daily total: " + total2);
        System.out.println("Open readers: " +
                TrackedReader.getOpenCount());

        // Demonstrating an incorrectly closed resource
        System.out.println("\nINTENTIONALLY LEAKY MANUAL VERSION");

        int total3 = generateReportLeakyManual(files);

        System.out.println("Daily total: " + total3);
        System.out.println("Open readers: " +
                TrackedReader.getOpenCount());
    }

    // Correct manual resource closing using finally
    static int generateReportWithFinally(List<Path> files) {

        int total = 0;

        for (Path file : files) {

            TrackedReader reader = null;
            int subtotal = 0;
            boolean success = false;

            try {

                reader = new TrackedReader(file);

                String line;

                while ((line = reader.readLine()) != null) {

                    line = line.trim();

                    if (!line.matches("\\d+")) {
                        throw new IOException(
                                "Invalid sales record: " + line);
                    }

                    subtotal += Integer.parseInt(line);
                }

                success = true;

            } catch (IOException e) {

                System.out.println("Failed reading " +
                        file.getFileName() + ": " + e.getMessage());

            } finally {

                if (reader != null) {

                    try {
                        reader.close();
                    } catch (IOException e) {

                        System.out.println("Could not close " +
                                file.getFileName());

                        success = false;
                    }
                }
            }

            if (success) {
                total += subtotal;
            }
        }

        return total;
    }

    // Automatic resource closing using try-with-resources
    static int generateReportWithTWR(List<Path> files) {

        int total = 0;

        for (Path file : files) {

            int subtotal = 0;

            try {

                try (TrackedReader reader = new TrackedReader(file)) {

                    String line;

                    while ((line = reader.readLine()) != null) {

                        line = line.trim();

                        if (!line.matches("\\d+")) {
                            throw new IOException(
                                    "Invalid sales record: " + line);
                        }

                        subtotal += Integer.parseInt(line);
                    }
                }

                total += subtotal;

            } catch (IOException e) {

                System.out.println("Failed reading " +
                        file.getFileName() + ": " + e.getMessage());
            }
        }

        return total;
    }

    // Incorrect manual closing used to demonstrate a resource leak
    static int generateReportLeakyManual(List<Path> files) {

        int total = 0;

        for (Path file : files) {

            TrackedReader reader = null;
            int subtotal = 0;

            try {

                reader = new TrackedReader(file);

                String line;

                while ((line = reader.readLine()) != null) {

                    line = line.trim();

                    if (!line.matches("\\d+")) {
                        throw new IOException(
                                "Invalid sales record: " + line);
                    }

                    subtotal += Integer.parseInt(line);
                }

                // This line is skipped if an exception occurs above
                reader.close();

                total += subtotal;

            } catch (IOException e) {

                System.out.println("Failed reading " +
                        file.getFileName() + ": " + e.getMessage());
            }
        }

        return total;
    }
}

// Helper class to track opened and closed file readers
class TrackedReader implements AutoCloseable {

    private final BufferedReader reader;
    private boolean closed = false;

    private static int openCount = 0;

    TrackedReader(Path file) throws IOException {

        reader = Files.newBufferedReader(file);
        openCount++;
    }

    String readLine() throws IOException {

        return reader.readLine();
    }

    @Override
    public void close() throws IOException {

        if (!closed) {

            reader.close();
            closed = true;
            openCount--;
        }
    }

    static int getOpenCount() {

        return openCount;
    }
}
