package com.example.logsapi.controller;

import com.example.logsapi.model.Log;
import com.example.logsapi.repository.LogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.*;
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

    /**
     * Flexible filtered fetch. Pass any combination of params.
     * Example: /api/logs?projectName=MyProj&appName=Web&level=ERROR&fromTs=2025-11-01T00:00:00&toTs=2025-11-06T00:00:00
     */
    @GetMapping
    public List<Log> getLogs(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String microservice,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTs,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTs
    ) {
        Specification<Log> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (projectName != null && !projectName.isEmpty()) {
                predicates.add(cb.equal(root.get("projectName"), projectName));
            }
            if (appName != null && !appName.isEmpty()) {
                predicates.add(cb.equal(root.get("appName"), appName));
            }
            if (microservice != null && !microservice.isEmpty()) {
                predicates.add(cb.equal(root.get("microservice"), microservice));
            }
            if (level != null && !level.isEmpty()) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (fromTs != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fromTs));
            }
            if (toTs != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), toTs));
            }
            // order latest first
            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec);
    }

    @GetMapping("/countByLevel")
    public Map<String, Long> countByLevel() {
        return repo.findAll()
                .stream()
                .collect(Collectors.groupingBy(Log::getLevel, Collectors.counting()));
    }

    @GetMapping("/distinctValues")
    public Map<String, Set<String>> distinctValues() {
        List<Log> all = repo.findAll();
        Set<String> projects = all.stream().map(Log::getProjectName).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> apps = all.stream().map(Log::getAppName).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> microservices = all.stream().map(Log::getMicroservice).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> levels = all.stream().map(Log::getLevel).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, Set<String>> result = new HashMap<>();
        result.put("projects", projects);
        result.put("apps", apps);
        result.put("microservices", microservices);
        result.put("levels", levels);
        return result;
    }

    @GetMapping("/test")
    public String testApi() {
        return "✅ Log Monitoring API is up and running!";
    }

    @PostMapping("/receive")
    public String receiveLog(@RequestBody String logMessage) {
        System.out.println("Received log: " + logMessage);
        return "Log received successfully!";
    }
}
