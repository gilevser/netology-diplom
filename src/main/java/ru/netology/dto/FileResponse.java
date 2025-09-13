package ru.netology.dto;

import java.util.Map;

public class FileResponse {
    private String message;
    private Map<String, Object> details;

    public FileResponse(String message) {
        this.message = message;
    }

    public FileResponse(String message, Map<String, Object> details) {
        this.message = message;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}