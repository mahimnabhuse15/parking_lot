package com.parking.auth.controller;

import com.parking.auth.model.User;
import com.parking.auth.service.JwtService;
import com.parking.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @Autowired
    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User registeredUser = userService.registerUser(request.getUsername(), request.getPassword(), request.getRole());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully!");
            response.put("username", registeredUser.getUsername());
            response.put("role", registeredUser.getRole());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = userService.loginAndGenerateToken(request.getUsername(), request.getPassword());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        boolean isValid = jwtService.isTokenValid(token);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        if (isValid) {
            response.put("username", jwtService.extractUsername(token));
            response.put("role", jwtService.extractClaim(token, claims -> claims.get("role", String.class)));
        }
        return ResponseEntity.ok(response);
    }

    // Static DTO classes for request binding
    public static class RegisterRequest {
        private String username;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
