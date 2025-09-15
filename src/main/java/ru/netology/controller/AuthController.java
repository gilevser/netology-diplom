package ru.netology.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.service.AuthService;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> authRequest) {
        String login = authRequest.get("login");
        String password = authRequest.get("password");

        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Bad credentials"));
        }

        Optional<String> token = authService.authenticate(login, password);
        if (token.isPresent()) {
            return ResponseEntity.ok(Map.of("auth-token", token.get()));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Bad credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> authRequest) {
        String login = authRequest.get("login");
        String password = authRequest.get("password");

        if (login == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Login and password required"));
        }

        if (authService.register(login, password)) {
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "User already exists"));
        }
    }
}