package com.example.logsapi.service;

import com.example.logsapi.model.ProjectPermission;
import com.example.logsapi.model.Role;
import com.example.logsapi.model.User;
import com.example.logsapi.repository.ProjectPermissionRepository;
import com.example.logsapi.repository.RoleRepository;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    private final ProjectPermissionRepository permRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserRoleRepository userRoleRepo;

    public AuthorizationService(ProjectPermissionRepository permRepo,
                                UserRepository userRepo,
                                RoleRepository roleRepo,
                                UserRoleRepository userRoleRepo) {
        this.permRepo = permRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
    }

    public boolean hasProjectRole(Long userId, String projectName, String role) {
        return permRepo.existsByUserIdAndProjectNameAndRole(userId, projectName, role);
    }

    public boolean isAdmin(Long userId) {
        Optional<Role> r = roleRepo.findByName("ADMIN");
        return r.isPresent() && userRoleRepo.existsByUserIdAndRoleId(userId, r.get().getId());
    }

    public boolean canViewProject(String username, String projectName) {
        Optional<User> u = userRepo.findByUsername(username);
        if (u.isEmpty()) return false;
        Long userId = u.get().getId();
        if (isAdmin(userId)) return true;
        return hasProjectRole(userId, projectName, "PROJECT_VIEWER")
                || hasProjectRole(userId, projectName, "PROJECT_EDITOR");
    }

    public List<String> getProjectNamesForUser(String username) {
        Optional<User> u = userRepo.findByUsername(username);
        if (u.isEmpty()) return List.of();
        Long userId = u.get().getId();
        return permRepo.findByUserId(userId)
                .stream()
                .map(ProjectPermission::getProjectName)
                .distinct()
                .collect(Collectors.toList());
    }
}
