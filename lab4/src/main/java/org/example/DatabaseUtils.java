package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseUtils {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/words";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "users";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void createTableIfNotExists(Connection connection) throws SQLException {
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

    public static void saveFileInfoToDatabase(Connection connection, FileInfo fileInfo) throws SQLException {
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
