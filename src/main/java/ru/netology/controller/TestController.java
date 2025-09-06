package ru.netology.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("status", "Server is running!");
    }

    @GetMapping("/cloud/test")
    public Map<String, String> cloudTest() {
        return Map.of("status", "Cloud API is working!");
    }
}