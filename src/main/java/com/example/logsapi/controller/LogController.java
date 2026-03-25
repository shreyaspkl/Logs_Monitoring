package com.example.logsapi.controller;

import com.example.logsapi.DTOs.LogCreateRequest;
import com.example.logsapi.model.EnvironmentType;
import com.example.logsapi.model.Log;
import com.example.logsapi.model.PermissionCode;
import com.example.logsapi.model.Project;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.LogRepository;
import com.example.logsapi.repository.ProjectRepository;
import com.example.logsapi.utility.RbacAuthorizationService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogRepository repo;
    private final ProjectRepository projectRepo;
    private final RbacAuthorizationService rbac;
    private final DateTimeFormatter LOCAL_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LogController(LogRepository repo, ProjectRepository projectRepo, RbacAuthorizationService rbac) {
        this.repo = repo;
        this.projectRepo = projectRepo;
        this.rbac = rbac;
    }

    // create log only if caller has LOG_WRITE on (project, env)
    @PreAuthorize("@rbac.hasPermission(authentication, #req.projectId, #req.environment.name(), 'LOG_WRITE')")
    @PostMapping
    public Log createLog(@Valid @RequestBody LogCreateRequest req) {
        Project project = projectRepo.findById(req.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        Log log = new Log();
        log.setProject(project);
        log.setEnvironment(req.getEnvironment());

        // backward-compat temporary field (if kept)
        log.setProjectName(project.getProjectKey());

        log.setAppName(req.getAppName());
        log.setMicroservice(req.getMicroservice());
        log.setSourceApp(req.getSourceApp());
        log.setLevel(req.getLevel());
        log.setMessage(req.getMessage());

        return repo.save(log);
    }

    @GetMapping
    public List<Log> getLogs(
            Authentication authentication,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) EnvironmentType environment,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String microservice,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String fromTs,
            @RequestParam(required = false) String toTs
    ) {
        Set<ScopeKey> allowedScopes = readableScopes(authentication);
        if (allowedScopes.isEmpty()) return List.of();

        LocalDateTime from = parseLocalDateTimeSafe(fromTs);
        LocalDateTime to = parseLocalDateTimeSafe(toTs);

        Specification<Log> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // RBAC scope predicate:
            // (project=A and env=DEV) OR (project=B and env=PROD) ...
            List<Predicate> scopePredicates = new ArrayList<>();
            for (ScopeKey s : allowedScopes) {
                scopePredicates.add(cb.and(
                        cb.equal(root.get("project").get("id"), s.projectId()),
                        cb.equal(root.get("environment"), s.environment())
                ));
            }
            predicates.add(cb.or(scopePredicates.toArray(new Predicate[0])));

            // optional filters
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (environment != null) {
                predicates.add(cb.equal(root.get("environment"), environment));
            }
            if (appName != null && !appName.isBlank()) {
                predicates.add(cb.equal(root.get("appName"), appName));
            }
            if (microservice != null && !microservice.isBlank()) {
                predicates.add(cb.equal(root.get("microservice"), microservice));
            }
            if (level != null && !level.isBlank()) {
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

    @GetMapping("/distinctValues")
    public Map<String, List<String>> distinctValues(Authentication authentication) {
        List<Log> authorized = filterAuthorizedLogs(repo.findAll(), authentication);

        List<String> projects = authorized.stream()
                .map(l -> l.getProject() != null ? l.getProject().getProjectKey() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<String> environments = authorized.stream()
                .map(l -> l.getEnvironment() != null ? l.getEnvironment().name() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<String> apps = authorized.stream().map(Log::getAppName).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> microservices = authorized.stream().map(Log::getMicroservice).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> levels = authorized.stream().map(Log::getLevel).filter(Objects::nonNull).distinct().sorted().toList();

        Map<String, List<String>> result = new HashMap<>();
        result.put("projects", projects);
        result.put("environments", environments);
        result.put("apps", apps);
        result.put("microservices", microservices);
        result.put("levels", levels);
        return result;
    }

    @GetMapping("/countByLevel")
    public Map<String, Long> countByLevel(Authentication authentication) {
        List<Log> authorized = filterAuthorizedLogs(repo.findAll(), authentication);
        return authorized.stream().collect(Collectors.groupingBy(Log::getLevel, Collectors.counting()));
    }

    // keep public health endpoint
    @GetMapping("/test")
    public String testApi() {
        return "Log Monitoring API is up and running!";
    }

    // optional: secure raw receive by role scope if you keep it
    @PostMapping("/receive")
    public String receiveLog(@RequestBody String logMessage) {
        System.out.println("Received log: " + logMessage);
        return "Log received successfully!";
    }

    private Set<ScopeKey> readableScopes(Authentication authentication) {
        return rbac.getBindings(authentication).stream()
                .filter(b -> b.getRole().getPermissions().stream()
                        .anyMatch(p -> p.getCode() == PermissionCode.LOG_READ))
                .map(b -> new ScopeKey(b.getProject().getId(), b.getEnvironment()))
                .collect(Collectors.toSet());
    }

    private List<Log> filterAuthorizedLogs(List<Log> logs, Authentication authentication) {
        Set<ScopeKey> allowed = readableScopes(authentication);
        if (allowed.isEmpty()) return List.of();

        return logs.stream()
                .filter(l -> l.getProject() != null && l.getEnvironment() != null)
                .filter(l -> allowed.contains(new ScopeKey(l.getProject().getId(), l.getEnvironment())))
                .toList();
    }

    private LocalDateTime parseLocalDateTimeSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) s = s + ":00";
            return LocalDateTime.parse(s, LOCAL_FMT);
        } catch (Exception ex) {
            return null;
        }
    }

    private record ScopeKey(Long projectId, EnvironmentType environment) {}
}