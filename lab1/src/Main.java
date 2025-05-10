import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[\\p{L}\\d']+\\b");

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        HashMap<String, Long> map = new HashMap<>();

        try (Stream<String> lines = Files.lines(Paths.get("resources/mprinc.txt"))) {
            lines.flatMap(line -> WORD_PATTERN.matcher(line).results().map(mr -> mr.group().toLowerCase()))
                    .filter(word -> !word.isEmpty())
                    .forEach(word -> map.merge(word, 1L, Long::sum));

            map.forEach((key, value) -> System.out.println(key + ": " + value));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        long endTime = System.nanoTime();
        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
    }
}