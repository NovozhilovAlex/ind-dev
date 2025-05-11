package org.example;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "WordCounterServlet", urlPatterns = {"/search", "/download"})
public class WordCounterServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String searchWord = request.getParameter("word");

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<title>Микро поисковик</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Результаты поиска</h1>");

        if (searchWord != null && !searchWord.isEmpty()) {
            try (Connection connection = DatabaseUtils.getConnection()) {
                List<FileInfoDTO> files = searchFiles(connection, searchWord);
                if (files.isEmpty()) {
                    out.println("<p>Файлов не найдо: " + searchWord + "</p>");
                } else {
                    out.println("<ul>");
                    for (FileInfoDTO file : files) {
                        String encodedFilename = URLEncoder.encode(file.getFilePath(), "UTF-8");
                        out.println("<li>" +
                                "<a href=\"download?filename=" + encodedFilename + "\">" +
                                file.getFilePath() +
                                "</a>" +
                                ", Количество: " + file.getCount() + "</li>");
                    }
                    out.println("</ul>");
                }
            } catch (SQLException e) {
                out.println("<p>Ошибка: " + e.getMessage() + "</p>");
            }
        } else {
            out.println("<p>Введите слово для поиска.</p>");
        }

        out.println("<a href=\"index.html\">Вернуться к поиску</a>");
        out.println("</body>");
        out.println("</html>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getServletPath();

        if ("/download".equals(action)) {
            downloadFile(request, response);
        }
    }

    private void downloadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String filename = request.getParameter("filename");
        if (filename == null || filename.isEmpty()) {
            response.getWriter().println("Файл не предоставлен");
            return;
        }

        String decodedFilename = java.net.URLDecoder.decode(filename, "UTF-8");
        ServletContext context = getServletContext();
        String resourcePath = "/" + decodedFilename.replace("\\", "/");
        InputStream fis = context.getResourceAsStream(resourcePath);

        if (fis == null) {
            response.getWriter().println("Файл не найден: " + resourcePath);
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + decodedFilename + "\"");

        try (OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } finally {
            fis.close();
        }
    }


    private List<FileInfoDTO> searchFiles(Connection connection, String searchWord) throws SQLException {
        List<FileInfoDTO> files = new ArrayList<>();
        String sql = "SELECT file_path, count FROM word2 WHERE value = ? ORDER BY count DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    FileInfoDTO fileInfo = new FileInfoDTO(resultSet.getString("file_path"));
                    fileInfo.setCount(resultSet.getLong("count"));
                    files.add(fileInfo);
                }
            }
        }
        return files;
    }
}