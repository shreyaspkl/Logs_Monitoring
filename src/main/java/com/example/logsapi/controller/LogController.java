package com.example.logsapi.controller;

import com.example.logsapi.model.Log;
import com.example.logsapi.repository.LogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    private final LogRepository repo;
    private final DateTimeFormatter LOCAL_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LogController(LogRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Log createLog(@RequestBody Log log) {
        return repo.save(log);
    }

    /**
     * Flexible filtered fetch. Accepts fromTs/toTs as local ISO strings like "2025-11-05T12:30" or "2025-11-05T12:30:00".
     */
    @GetMapping
    public List<Log> getLogs(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String microservice,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String fromTs,
            @RequestParam(required = false) String toTs
    ) {
        // parse dates if present
        LocalDateTime from = parseLocalDateTimeSafe(fromTs);
        LocalDateTime to = parseLocalDateTimeSafe(toTs);

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
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec);
    }

    /**
     * Returns distinct values as arrays so frontend receives JSON arrays reliably.
     */
    @GetMapping("/distinctValues")
    public Map<String, List<String>> distinctValues() {
        List<Log> all = repo.findAll();

        List<String> projects = all.stream()
                .map(Log::getProjectName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> apps = all.stream()
                .map(Log::getAppName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> microservices = all.stream()
                .map(Log::getMicroservice)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> levels = all.stream()
                .map(Log::getLevel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, List<String>> result = new HashMap<>();
        result.put("projects", projects);
        result.put("apps", apps);
        result.put("microservices", microservices);
        result.put("levels", levels);
        return result;
    }

    private LocalDateTime parseLocalDateTimeSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        // Accept input like "2025-11-05T12:30" or "2025-11-05T12:30:00"
        try {
            // Ensure seconds exist; ISO_LOCAL_DATE_TIME accepts "HH:mm:ss" or "HH:mm"
            if (s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
                s = s + ":00";
            }
            return LocalDateTime.parse(s, LOCAL_FMT);
        } catch (Exception ex) {
            // parsing failed; return null so filter is ignored
            return null;
        }
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
        System.out.println("Received log: " + logMessage);
        return "Log received successfully!";
    }
}
