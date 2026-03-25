package com.example.logsapi.controller;

import com.example.logsapi.DTOs.AssignAccessRequestDto;
import com.example.logsapi.DTOs.RevokeAccessRequestDto;
import com.example.logsapi.model.EnvironmentType;
import com.example.logsapi.model.Project;
import com.example.logsapi.model.Role;
import com.example.logsapi.model.User;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.ProjectRepository;
import com.example.logsapi.repository.RoleRepository;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import com.example.logsapi.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.logsapi.utility.AccessAdminService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access")
public class AccessAdminController {

    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;
    private final RoleRepository roleRepo;
    private final UserProjectRoleBindingRepository bindingRepo;
    private final AccessAdminService accessAdminService;
    public AccessAdminController(UserRepository userRepo,
                                 ProjectRepository projectRepo,
                                 RoleRepository roleRepo,
                                 UserProjectRoleBindingRepository bindingRepo,
                                 AccessAdminService accessAdminService) {
        this.userRepo = userRepo;
        this.projectRepo = projectRepo;
        this.roleRepo = roleRepo;
        this.bindingRepo = bindingRepo;
        this.accessAdminService = accessAdminService;
    }

    @PreAuthorize("@rbac.hasPermission(authentication, #req.projectId, #req.environment.name(), 'ACCESS_MANAGE')")
    @PostMapping("/assign")
    public ResponseEntity<?> assign(@Valid @RequestBody AssignAccessRequestDto req) {
        Project project = requireValidProject(req.getProjectId());
        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = roleRepo.findByName(req.getRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        UserProjectRoleBinding binding = new UserProjectRoleBinding();
        binding.setUser(user);
        binding.setProject(project);
        binding.setEnvironment(req.getEnvironment());
        binding.setRole(role);

        bindingRepo.save(binding);
        return ResponseEntity.ok(Map.of("message", "assigned"));
    }

    @PreAuthorize("@rbac.hasPermission(authentication, #req.projectId, #req.environment.name(), 'ACCESS_MANAGE')")
    @DeleteMapping("/revoke")
    public ResponseEntity<?> revoke(@Valid @RequestBody RevokeAccessRequestDto req) {
        Project project = requireValidProject(req.getProjectId());
        Role role = roleRepo.findByName(req.getRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long deleted = accessAdminService.revoke(
                user.getId(), project.getId(), req.getEnvironment(), role.getId()
        );

        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No matching access binding found"));
        }

        return ResponseEntity.ok(Map.of("message", "revoked", "deletedCount", deleted));
    }

    @PreAuthorize("@rbac.hasPermission(authentication, #projectId, #environment.name(), 'ACCESS_MANAGE')")
    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam Long projectId, @RequestParam EnvironmentType environment) {
        Project project = requireValidProject(projectId);

        List<AccessView> result = bindingRepo.findAll().stream()
                .filter(b -> b.getProject().getId().equals(project.getId()))
                .filter(b -> b.getEnvironment() == environment)
                .map(b -> new AccessView(
                        b.getUser().getUsername(),
                        b.getProject().getId(),
                        b.getProject().getProjectKey(),
                        b.getEnvironment().name(),
                        b.getRole().getName()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    private Project requireValidProject(Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "projectId is required and must reference an existing project"
            );
        }
        return projectRepo.findById(projectId).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "projectId is required and must reference an existing project"
                ));
    }

    public record AccessView(
            String username,
            Long projectId,
            String projectKey,
            String environment,
            String role
    ) {}
}