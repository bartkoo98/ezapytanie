package bartkoo98.com.github.ezapytanie.controller;

import bartkoo98.com.github.ezapytanie.dto.request.LoginRequest;
import bartkoo98.com.github.ezapytanie.dto.request.RegisterRequest;
import bartkoo98.com.github.ezapytanie.dto.response.LoginResponse;
import bartkoo98.com.github.ezapytanie.exception.EmailAlreadyExistsException;
import bartkoo98.com.github.ezapytanie.exception.InvalidCredentialsException;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User created = authService.register(request);
            return ResponseEntity.status(201).body(Map.of(
                    "id", created.getId(),
                    "email", created.getEmail(),
                    "role", created.getRole().name()
            ));
        } catch (EmailAlreadyExistsException ex) {
            return ResponseEntity.status(409).body(Map.of(
                    "code", "EMAIL_CONFLICT",
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", "INVALID_CREDENTIALS",
                    "message", ex.getMessage()
            ));
        }
    }
}
