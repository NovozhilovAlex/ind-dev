import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[\\p{L}\\d']+\\b");
    private static final String DIRECTORY = "resources/files";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/words";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "users";

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.nanoTime();

        BlockingQueue<FileInfo> resultsQueue = new LinkedBlockingQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (Stream<Path> files = Files.list(Paths.get(DIRECTORY))) {
            files.forEach(filePath -> executor.submit(() -> {
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
            }));
        } catch (IOException ex) {
            System.err.println("Error dir access: " + ex.getMessage());
        }

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.HOURS);

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            createTableIfNotExists(connection);

            while (!resultsQueue.isEmpty()) {
                FileInfo fileInfo = resultsQueue.take();
                saveFileInfoToDatabase(connection, fileInfo);
            }

        } catch (SQLException | InterruptedException e) {
            System.err.println("Error db processing: " + e.getMessage());
        }

        long endTime = System.nanoTime();
        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
    }


    private static void createTableIfNotExists(Connection connection) throws SQLException {
        String createTableSql = "CREATE TABLE IF NOT EXISTS word2 (" +
                "id BIGSERIAL PRIMARY KEY," +
                "value character varying NOT NULL," +
                "count bigint NOT NULL," +
                "file_path character varying NOT NULL" +
                ");";
        try (PreparedStatement preparedStatement = connection.prepareStatement(createTableSql)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            System.err.println("Error db creating: " + e.getMessage());
        }
        String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_word_value ON word2 (value);";
        try (PreparedStatement preparedStatement = connection.prepareStatement(createIndexSql)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            System.err.println("Error index creating: " + e.getMessage());
        }
    }

    private static void saveFileInfoToDatabase(Connection connection, FileInfo fileInfo) throws SQLException {
        String insertSQL = "INSERT INTO word2 (value, count, file_path) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            for (ConcurrentHashMap.Entry<String, AtomicLong> entry : fileInfo.getMap().entrySet()) {
                String word = entry.getKey();
                long count = entry.getValue().get();
                String filePath = fileInfo.getFilePath();

                preparedStatement.setString(1, word);
                preparedStatement.setLong(2, count);
                preparedStatement.setString(3, filePath);
                preparedStatement.executeUpdate();
            }
        }
    }
}
