package com.parking.auth.service;

import com.parking.auth.model.User;
import com.parking.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User registerUser(String username, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists!");
        }

        // Hashing the raw password securely with BCrypt
        String hashedPassword = passwordEncoder.encode(password);
        String finalRole = (role == null || role.trim().isEmpty()) ? "ROLE_USER" : role;

        User user = new User(username, hashedPassword, finalRole);
        return userRepository.save(user);
    }

    public String loginAndGenerateToken(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid username or password!");
        }

        User user = userOpt.get();
        // Check if raw password matches the stored BCrypt hash
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password!");
        }

        // Return a fresh signed JWT token
        return jwtService.generateToken(user.getUsername(), user.getRole());
    }
}
