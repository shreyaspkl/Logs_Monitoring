package com.example.logsapi.controller;

import com.example.logsapi.DTOs.LoginRequestDto;
import com.example.logsapi.DTOs.LoginResponseDto;
import com.example.logsapi.DTOs.SignupResponseDto;
import com.example.logsapi.model.User;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.utility.AuthService;
import com.example.logsapi.utility.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"https://log-monitoring-frontend.onrender.com", "http://localhost:3000"})
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private  AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody LoginRequestDto loginRequestDto){
        return ResponseEntity.ok(authService.signup(loginRequestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }
}
