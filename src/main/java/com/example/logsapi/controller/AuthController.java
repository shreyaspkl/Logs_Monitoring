package com.example.logsapi.controller;

import com.example.logsapi.model.User;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.utility.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"https://log-monitoring-frontend.onrender.com", "http://localhost:3000"})
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

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

        // optionally auto-login after register: generate JWT
        String token = jwtService.generateToken(username);
        Map<String,Object> res = new HashMap<>();
        res.put("message", "registered");
        res.put("token", token);
        return ResponseEntity.ok(res);
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
}
