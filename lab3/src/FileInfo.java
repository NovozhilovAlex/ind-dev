import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FileInfo {
    private final String filePath;
    private final ConcurrentHashMap<String, AtomicLong> map;

    public FileInfo(String filePath) {
        this.filePath = filePath;
        map = new ConcurrentHashMap<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public ConcurrentHashMap<String, AtomicLong> getMap() {
        return map;
    }
}

