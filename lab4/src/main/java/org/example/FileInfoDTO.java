package org.example;

public class FileInfoDTO {
    private String filePath;
    private long count;

    public FileInfoDTO(String filePath) {
        this.filePath = filePath;
        this.count = 0;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
