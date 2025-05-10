import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[\\p{L}\\d']+\\b");
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final String DIRECTORY = "resources/files";

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.nanoTime();

        BlockingQueue<FileInfo> resultsQueue = new LinkedBlockingQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        try (Stream<Path> files = Files.list(Paths.get(DIRECTORY))) {
            files.forEach(filePath -> {

                executor.submit(() -> {
                    FileInfo fileInfo = new FileInfo(filePath.toString());
                    try (Stream<String> lines = Files.lines(filePath)) {
                        lines.flatMap(line -> WORD_PATTERN.matcher(line).results().map(mr -> mr.group().toLowerCase()))
                                .filter(word -> !word.isEmpty())
                                .forEach(word -> fileInfo.getMap()
                                        .computeIfAbsent(word, k -> new AtomicLong(0)).incrementAndGet());

                        resultsQueue.put(fileInfo);
                    } catch (IOException | InterruptedException ex) {
                        System.err.println("Error processing file " + filePath + ": " + ex.getMessage());
                    }
                });
            });
        } catch (IOException ex) {
            System.err.println("Error dir access: " + ex.getMessage());
        }

        executor.shutdown();
        executor.awaitTermination(6, TimeUnit.HOURS);

        List<FileInfo> allFileInfos = new ArrayList<>();
        int fileCount = getFileCount();
        for (int i = 0; i < fileCount; i++) {
            try {
                FileInfo fileInfo = resultsQueue.take();
                allFileInfos.add(fileInfo);
            } catch (InterruptedException e) {
                System.err.println("Error queue access: " + e.getMessage());
            }
        }

        allFileInfos.forEach(fileInfo -> {
            System.out.println("File: " + fileInfo.getFilePath());
            fileInfo.getMap().forEach((key, value) -> System.out.println("  " + key + ": " + value));
            System.out.println();
        });

        long endTime = System.nanoTime();
        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
    }

    private static int getFileCount() {
        try (Stream<Path> files = Files.list(Paths.get(Main2.DIRECTORY))) {
            return (int) files.count();
        } catch (IOException e) {
            System.err.println("Ошибка при подсчете файлов в директории: " + e.getMessage());
            return 0;
        }
    }
}