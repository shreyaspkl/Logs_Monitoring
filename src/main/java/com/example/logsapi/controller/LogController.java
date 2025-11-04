package com.example.logsapi.controller;

import com.example.logsapi.model.Log;
import com.example.logsapi.repository.LogRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    private final LogRepository repo;

    public LogController(LogRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Log createLog(@RequestBody Log log) {
        return repo.save(log);
    }

    @GetMapping
    public List<Log> getLogs() {
        return repo.findAll();
    }
    @GetMapping("/countByLevel")
    public Map<String, Long> countByLevel() {
        return repo.findAll()
                .stream()
                .collect(Collectors.groupingBy(Log::getLevel, Collectors.counting()));
    }

    @GetMapping("/test")
    public String testApi() {
        return "✅ Log Monitoring API is up and running!";
    }

    @PostMapping("/receive")
    public String receiveLog(@RequestBody String logMessage) {
        // For now, just print it — later we’ll save to PostgreSQL
        System.out.println("Received log: " + logMessage);
        return "Log received successfully!";
    }
}
