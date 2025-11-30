package com.example.logsapi.controller;

import com.example.logsapi.model.User;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.utility.JwtService;
import com.example.logsapi.service.AuthorizationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"https://log-monitoring-frontend.onrender.com", "http://localhost:3000"})
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @Autowired private AuthorizationService authorizationService;

    // Register (create account)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, email and password are required"));
        }
        if (userRepo.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "username already exists"));
        }
        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "email already exists"));
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        userRepo.save(u);

        // auto-login after register
        String token = jwtService.generateToken(username);
        return ResponseEntity.ok(Map.of(
                "message", "registered",
                "token", token
        ));
    }

    // Login (sign in)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        }

        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error","invalid credentials"));
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(403).body(Map.of("error","invalid credentials"));
        }

        String token = jwtService.generateToken(username);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // Who am I?
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthenticated"));
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }

        User u = userOpt.get();

        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "createdAt", u.getCreatedAt()
        ));
    }

    // List projects the user has permission to view
    @GetMapping("/projects")
    public ResponseEntity<?> myProjects(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error","unauthenticated"));
        }

        String username = authentication.getName();
        List<String> projects = authorizationService.getProjectNamesForUser(username);
        return ResponseEntity.ok(Map.of("projects", projects));
    }
}
