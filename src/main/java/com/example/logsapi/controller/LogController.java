package com.example.logsapi.controller;

import com.example.logsapi.model.Log;
import com.example.logsapi.model.User;
import com.example.logsapi.repository.LogRepository;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.service.AuthorizationService;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    private final LogRepository repo;
    private final UserRepository userRepo;
    private final AuthorizationService authorizationService;

    private final DateTimeFormatter LOCAL_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LogController(LogRepository repo, UserRepository userRepo, AuthorizationService authorizationService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public Log createLog(@RequestBody Log log) {
        return repo.save(log);
    }

    /**
     * GET /api/logs — with RBAC check added
     */
    @GetMapping
    public ResponseEntity<?> getLogs(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String microservice,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String fromTs,
            @RequestParam(required = false) String toTs,
            Authentication auth
    ) {

        // Must be logged in to fetch logs
        if (auth == null) {
            return ResponseEntity.status(403).body(Map.of("error", "unauthenticated"));
        }

        String username = auth.getName();
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error","invalid user"));
        }

        Long userId = user.getId();

        // ***********************
        // 🔐 Project Access Check
        // ***********************

        if (projectName != null && !projectName.isEmpty()) {
            if (!authorizationService.canViewProject(username, projectName)) {
                return ResponseEntity.status(403).body(
                        Map.of("error", "No access to project: " + projectName)
                );
            }
        } else {
            // No project specified — only ADMIN can view all logs
            if (!authorizationService.isAdmin(userId)) {
                return ResponseEntity.status(403).body(
                        Map.of("error", "Specify a project or be ADMIN")
                );
            }
        }

        // ***********************
        // Existing Filtering Logic
        // ***********************
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

        return ResponseEntity.ok(repo.findAll(spec));
    }

    @GetMapping("/distinctValues")
    public Map<String, List<String>> distinctValues() {
        List<Log> all = repo.findAll();

        Map<String, List<String>> result = new HashMap<>();
        result.put("projects", distinct(all, Log::getProjectName));
        result.put("apps", distinct(all, Log::getAppName));
        result.put("microservices", distinct(all, Log::getMicroservice));
        result.put("levels", distinct(all, Log::getLevel));
        return result;
    }

    private List<String> distinct(List<Log> all, java.util.function.Function<Log, String> getter) {
        return all.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private LocalDateTime parseLocalDateTimeSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            if (s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
                s = s + ":00";
            }
            return LocalDateTime.parse(s, LOCAL_FMT);
        } catch (Exception ex) {
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
        return "API Running!";
    }

    @PostMapping("/receive")
    public String receiveLog(@RequestBody String logMessage) {
        System.out.println("Received log: " + logMessage);
        return "Log received successfully!";
    }
}
