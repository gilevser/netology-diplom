package ru.netology.dto;

public class RenameRequest {
    private String filename;

    public RenameRequest() {}

    public RenameRequest(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

