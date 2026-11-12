import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResultProcessor {

    // Stores successful student results
    static class StudentResult {

        String studentId;
        int score;
        String result;

        StudentResult(String studentId, int score, String result) {
            this.studentId = studentId;
            this.score = score;
            this.result = result;
        }
    }

    // Stores failed file information
    static class FailureRecord {

        String fileName;
        String reason;

        FailureRecord(String fileName, String reason) {
            this.fileName = fileName;
            this.reason = reason;
        }

        public String toString() {
            return fileName + " : " + reason;
        }
    }

    // Stores the summary of one execution
    static class RunSummary {

        int successes;
        int failures;
        long elapsedNanos;
        List<FailureRecord> sampleFailures;

        RunSummary(int successes, int failures,
                   long elapsedNanos,
                   List<FailureRecord> sampleFailures) {

            this.successes = successes;
            this.failures = failures;
            this.elapsedNanos = elapsedNanos;
            this.sampleFailures = sampleFailures;
        }
    }

    // Task for processing one student file
    static class ScoreTask implements Callable<Void> {

        private final Path file;

        private final ConcurrentLinkedQueue<StudentResult> successes;
        private final ConcurrentLinkedQueue<FailureRecord> failures;

        ScoreTask(Path file,
                  ConcurrentLinkedQueue<StudentResult> successes,
                  ConcurrentLinkedQueue<FailureRecord> failures) {

            this.file = file;
            this.successes = successes;
            this.failures = failures;
        }

        private String readField(String line, String field) {

            String prefix = field + "=";

            if (!line.startsWith(prefix)) {
                throw new IllegalArgumentException(
                    "Missing or invalid " + field
                );
            }

            return line.substring(prefix.length()).trim();
        }

        public Void call() {

            try {

                // Read student file
                List<String> lines = Files.readAllLines(
                    file, StandardCharsets.UTF_8
                );

                // Check required data
                if (lines.size() < 2) {
                    throw new IllegalArgumentException(
                        "File does not contain all required information"
                    );
                }

                String studentId =
                    readField(lines.get(0), "studentId");

                String scoreText =
                    readField(lines.get(1), "score");

                if (studentId.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Student ID is empty"
                    );
                }

                // Convert score into an integer
                int score = Integer.parseInt(scoreText);

                // Validate score range
                if (score < 0 || score > 100) {
                    throw new IllegalArgumentException(
                        "Score must be between 0 and 100"
                    );
                }

                // Calculate student result
                String result;

                if (score >= 40) {
                    result = "PASS";
                } else {
                    result = "FAIL";
                }

                // Store successful result
                successes.add(
                    new StudentResult(studentId, score, result)
                );

            } catch (NoSuchFileException e) {

                failures.add(
                    new FailureRecord(
                        file.getFileName().toString(),
                        "File is missing"
                    )
                );

            } catch (NumberFormatException e) {

                failures.add(
                    new FailureRecord(
                        file.getFileName().toString(),
                        "Invalid score format"
                    )
                );

            } catch (IOException e) {

                failures.add(
                    new FailureRecord(
                        file.getFileName().toString(),
                        "File reading error: " + e.getMessage()
                    )
                );

            } catch (IllegalArgumentException e) {

                failures.add(
                    new FailureRecord(
                        file.getFileName().toString(),
                        "Invalid file data: " + e.getMessage()
                    )
                );
            }

            return null;
        }
    }

    // Creates a standard filename for each student
    static String fileName(int id) {

        return String.format("student-%05d.txt", id);
    }

    // Writes a valid student score file
    static void writeScoreFile(Path folder, int id, int score)
            throws IOException {

        List<String> data = Arrays.asList(
            "studentId=STU" + id,
            "score=" + score
        );

        Files.write(
            folder.resolve(fileName(id)),
            data,
            StandardCharsets.UTF_8
        );
    }

    // Creates a small hand-checked dataset
    static void prepareSmallDataset(Path folder)
            throws IOException {

        Files.createDirectories(folder);

        writeScoreFile(folder, 1, 80);
        writeScoreFile(folder, 2, 65);
        writeScoreFile(folder, 3, 35);

        // Create one corrupted file
        Files.write(
            folder.resolve(fileName(4)),
            Arrays.asList(
                "studentId=STU4",
                "score=INVALID"
            ),
            StandardCharsets.UTF_8
        );

        // Ensure the fifth file is missing
        Files.deleteIfExists(folder.resolve(fileName(5)));
    }

    // Creates 10,000 demo file tasks
    static void prepareLargeDataset(Path folder, int total)
            throws IOException {

        Files.createDirectories(folder);

        for (int i = 1; i <= total; i++) {

            Path file = folder.resolve(fileName(i));

            // Create 10 missing files
            if (i % 1000 == 0) {

                Files.deleteIfExists(file);
                continue;
            }

            // Create approximately 10 corrupted files
            if (i % 997 == 0) {

                Files.write(
                    file,
                    Arrays.asList(
                        "studentId=STU" + i,
                        "score=INVALID"
                    ),
                    StandardCharsets.UTF_8
                );

            } else {

                int score = (i * 37) % 101;

                writeScoreFile(folder, i, score);
            }
        }
    }

    // Processes files using a fixed thread pool
    static RunSummary processFiles(
            Path folder, int totalFiles, int poolSize)
            throws InterruptedException {

        ConcurrentLinkedQueue<StudentResult> successes =
            new ConcurrentLinkedQueue<>();

        ConcurrentLinkedQueue<FailureRecord> failures =
            new ConcurrentLinkedQueue<>();

        ExecutorService executor =
            Executors.newFixedThreadPool(poolSize);

        CompletionService<Void> completion =
            new ExecutorCompletionService<>(executor);

        // Maps submitted Future objects to their filenames
        Map<Future<Void>, String> fileNames = new HashMap<>();

        long startTime = System.nanoTime();

        try {

            // Submit all file-processing tasks
            for (int i = 1; i <= totalFiles; i++) {

                Path file = folder.resolve(fileName(i));

                Future<Void> future = completion.submit(
                    new ScoreTask(file, successes, failures)
                );

                fileNames.put(future, fileName(i));
            }

            // No more tasks will be submitted
            executor.shutdown();

            // Collect all completed tasks
            for (int i = 0; i < totalFiles; i++) {

                Future<Void> future = completion.take();

                try {

                    // Confirm task completion
                    future.get();

                } catch (ExecutionException e) {

                    // Handle an unexpected task failure
                    Throwable cause = e.getCause();

                    failures.add(
                        new FailureRecord(
                            fileNames.get(future),
                            "Unexpected error: " + cause
                        )
                    );
                }
            }

        } finally {

            executor.shutdownNow();
        }

        long elapsedTime = System.nanoTime() - startTime;

        // Keep a few failure examples for the report
        List<FailureRecord> samples = new ArrayList<>();

        int count = 0;

        for (FailureRecord f : failures) {

            if (count == 3) {
                break;
            }

            samples.add(f);
            count++;
        }

        return new RunSummary(
            successes.size(),
            failures.size(),
            elapsedTime,
            samples
        );
    }

    public static void main(String[] args)
            throws Exception {

        System.out.println("STAGE 1: SMALL DATASET VERIFICATION");

        Path smallFolder = Paths.get("q7-small-data");

        prepareSmallDataset(smallFolder);

        RunSummary small = processFiles(
            smallFolder, 5, 2
        );

        System.out.println("Successful files: " + small.successes);
        System.out.println("Failed files: " + small.failures);

        if (small.successes == 3 && small.failures == 2) {

            System.out.println("Small dataset verification: PASSED");

        } else {

            System.out.println("Small dataset verification: FAILED");
        }

        System.out.println();

        System.out.println("STAGE 2: LARGE DATASET PERFORMANCE TEST");

        int totalFiles = 10000;

        Path largeFolder = Paths.get("q7-score-data");

        prepareLargeDataset(largeFolder, totalFiles);

        int[] poolSizes = {1, 2, 4, 8};

        int repetitions = 3;

        for (int poolSize : poolSizes) {

            long[] times = new long[repetitions];

            RunSummary lastRun = null;

            System.out.println(
                "Testing with " + poolSize + " threads"
            );

            for (int r = 0; r < repetitions; r++) {

                lastRun = processFiles(
                    largeFolder, totalFiles, poolSize
                );

                times[r] = lastRun.elapsedNanos;
            }

            // Calculate median execution time
            Arrays.sort(times);

            double medianMillis =
                times[repetitions / 2] / 1000000.0;

            System.out.println(
                "Successful files: " + lastRun.successes
            );

            System.out.println(
                "Failed files: " + lastRun.failures
            );

            System.out.printf(
                "Median execution time: %.3f ms%n",
                medianMillis
            );

            System.out.println(
                "-------------------------------"
            );
        }

        System.out.println("Processing completed.");
    }
}
