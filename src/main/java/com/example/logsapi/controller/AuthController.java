package com.example.logsapi.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.example.logsapi.model.User;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.utility.JwtService;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (userRepo.existsByUsername(user.getUsername()))
            return ResponseEntity.badRequest().body("Username exists!");

        user.setPasswordHash(encoder.encode(user.getPasswordHash()));
        userRepo.save(user);

        return ResponseEntity.ok("User registered!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        String username = req.get("username");
        String password = req.get("password");

        User user = userRepo.findByUsername(username)
                .orElse(null);

        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtService.generateToken(username);

        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return ResponseEntity.ok(auth.getName());
    }
}
