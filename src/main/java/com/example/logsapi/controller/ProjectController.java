package com.example.logsapi.controller;

import com.example.logsapi.DTOs.ProjectResponseDto;
import com.example.logsapi.model.Project;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.ProjectRepository;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final UserProjectRoleBindingRepository bindingRepository;

    public ProjectController(ProjectRepository projectRepository,
                             UserProjectRoleBindingRepository bindingRepository) {
        this.projectRepository = projectRepository;
        this.bindingRepository = bindingRepository;
    }

    @GetMapping
    public List<ProjectResponseDto> listProjects(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "VIEWER") String requiredRole
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }

        RoleLevel threshold = RoleLevel.from(requiredRole);

        List<UserProjectRoleBinding> bindings = bindingRepository.findByUserUsername(authentication.getName());
        if (bindings.isEmpty()) {
            return List.of();
        }

        Map<Long, RoleLevel> maxRoleByProject = bindings.stream()
                .filter(b -> b.getProject() != null && b.getRole() != null && b.getRole().getName() != null)
                .collect(Collectors.toMap(
                        b -> b.getProject().getId(),
                        b -> RoleLevel.from(b.getRole().getName()),
                        RoleLevel::max
                ));

        Set<Long> allowedProjectIds = maxRoleByProject.entrySet().stream()
                .filter(e -> e.getValue().isAtLeast(threshold))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (allowedProjectIds.isEmpty()) {
            return List.of();
        }

        return projectRepository.findAllById(allowedProjectIds).stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/requestable")
    public List<ProjectResponseDto> listRequestableProjects(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }

        return projectRepository.findAll().stream()
                .filter(Project::isActive)
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/scopes")
    public List<ProjectScopeDto> listProjectScopes(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }

        List<UserProjectRoleBinding> bindings = bindingRepository.findByUserUsername(authentication.getName());
        if (bindings.isEmpty()) {
            return List.of();
        }

        Map<Long, List<UserProjectRoleBinding>> bindingsByProject = bindings.stream()
                .filter(b -> b.getProject() != null && b.getRole() != null && b.getRole().getName() != null)
                .filter(b -> RoleLevel.from(b.getRole().getName()).isAtLeast(RoleLevel.VIEWER))
                .collect(Collectors.groupingBy(b -> b.getProject().getId()));

        return bindingsByProject.values().stream()
                .map(projectBindings -> {
                    Project project = projectBindings.get(0).getProject();
                    List<String> environments = projectBindings.stream()
                            .map(binding -> binding.getEnvironment().name())
                            .distinct()
                            .sorted()
                            .toList();

                    String name = (project.getName() == null || project.getName().isBlank())
                            ? project.getProjectKey()
                            : project.getName();

                    return new ProjectScopeDto(
                            project.getId(),
                            project.getProjectKey(),
                            name,
                            environments
                    );
                })
                .toList();
    }

    private ProjectResponseDto toDto(Project project) {
        String name = (project.getName() == null || project.getName().isBlank())
                ? project.getProjectKey()
                : project.getName();
        return new ProjectResponseDto(project.getId(), project.getProjectKey(), name);
    }

    private enum RoleLevel {
        VIEWER,
        OPERATOR,
        ADMIN;

        static RoleLevel from(String role) {
            if (role == null || role.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "requiredRole must be one of VIEWER, OPERATOR, ADMIN");
            }
            try {
                return RoleLevel.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "requiredRole must be one of VIEWER, OPERATOR, ADMIN");
            }
        }

        boolean isAtLeast(RoleLevel required) {
            return this.ordinal() >= required.ordinal();
        }

        static RoleLevel max(RoleLevel a, RoleLevel b) {
            return a.ordinal() >= b.ordinal() ? a : b;
        }
    }

    public record ProjectScopeDto(
            Long id,
            String projectKey,
            String name,
            List<String> environments
    ) {}
}
