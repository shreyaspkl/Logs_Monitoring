package com.example.logsapi.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    // ✅ Simple GET API to test if backend works
    @GetMapping("/test")
    public String testApi() {
        return "✅ Log Monitoring API is up and running!";
    }

    // ✅ POST API to receive logs (from any app or service)
    @PostMapping("/receive")
    public String receiveLog(@RequestBody String logMessage) {
        // For now, just print it — later we’ll save to PostgreSQL
        System.out.println("Received log: " + logMessage);
        return "Log received successfully!";
    }
}
