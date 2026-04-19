package com.example.logsapi.controller;

import com.example.logsapi.DTOs.CreateAccessRequestDto;
import com.example.logsapi.DTOs.ReviewAccessRequestDto;
import com.example.logsapi.model.AccessRequest;
import com.example.logsapi.model.AccessRequestStatus;
import com.example.logsapi.model.Project;
import com.example.logsapi.model.Role;
import com.example.logsapi.model.User;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.AccessRequestRepository;
import com.example.logsapi.repository.ProjectRepository;
import com.example.logsapi.repository.RoleRepository;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import com.example.logsapi.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/access-requests")
public class AccessRequestController {

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final RoleRepository roleRepository;
    private final UserProjectRoleBindingRepository bindingRepository;

    public AccessRequestController(
            AccessRequestRepository accessRequestRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            RoleRepository roleRepository,
            UserProjectRoleBindingRepository bindingRepository
    ) {
        this.accessRequestRepository = accessRequestRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.roleRepository = roleRepository;
        this.bindingRepository = bindingRepository;
    }

    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateAccessRequestDto req) {
        User requester = requireCurrentUser(authentication);
        Project project = requireProject(req.getProjectId());
        Role requestedRole = requireRole(req.getRoleName());

        boolean alreadyBound = bindingRepository.existsByUserIdAndProjectIdAndEnvironmentAndRoleId(
                requester.getId(),
                project.getId(),
                req.getEnvironment(),
                requestedRole.getId()
        );
        if (alreadyBound) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Access already exists for this scope");
        }

        boolean duplicatePending = accessRequestRepository.existsByRequesterIdAndProjectIdAndEnvironmentAndRequestedRoleIdAndStatus(
                requester.getId(),
                project.getId(),
                req.getEnvironment(),
                requestedRole.getId(),
                AccessRequestStatus.PENDING
        );
        if (duplicatePending) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending request already exists for this scope");
        }

        AccessRequest request = new AccessRequest();
        request.setRequester(requester);
        request.setProject(project);
        request.setEnvironment(req.getEnvironment());
        request.setRequestedRole(requestedRole);
        request.setReason(trimToNull(req.getReason()));

        accessRequestRepository.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(request));
    }

    @GetMapping("/mine")
    public List<AccessRequestView> mine(Authentication authentication) {
        User requester = requireCurrentUser(authentication);
        return accessRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/pending")
    public List<AccessRequestView> pending(Authentication authentication) {
        User currentUser = requireCurrentUser(authentication);

        List<Long> manageableProjectIds = bindingRepository.findByUserUsername(currentUser.getUsername()).stream()
                .filter(binding -> binding.getRole() != null)
                .filter(binding -> binding.getRole().getPermissions().stream()
                        .anyMatch(permission -> "ACCESS_MANAGE".equals(permission.getCode().name())))
                .map(binding -> binding.getProject().getId())
                .distinct()
                .toList();

        if (manageableProjectIds.isEmpty()) {
            return List.of();
        }

        return accessRequestRepository
                .findByStatusAndProjectIdInOrderByCreatedAtDesc(AccessRequestStatus.PENDING, manageableProjectIds)
                .stream()
                .filter(request -> hasManagePermissionForRequest(authentication, request))
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @PreAuthorize("@rbac.hasManagePermissionForAccessRequest(authentication, #requestId)")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approve(
            Authentication authentication,
            @PathVariable Long requestId,
            @RequestBody(required = false) ReviewAccessRequestDto req
    ) {
        User reviewer = requireCurrentUser(authentication);
        AccessRequest request = requirePendingRequest(requestId);

        boolean alreadyBound = bindingRepository.existsByUserIdAndProjectIdAndEnvironmentAndRoleId(
                request.getRequester().getId(),
                request.getProject().getId(),
                request.getEnvironment(),
                request.getRequestedRole().getId()
        );

        if (!alreadyBound) {
            UserProjectRoleBinding binding = new UserProjectRoleBinding();
            binding.setUser(request.getRequester());
            binding.setProject(request.getProject());
            binding.setEnvironment(request.getEnvironment());
            binding.setRole(request.getRequestedRole());
            bindingRepository.save(binding);
        }

        request.setStatus(AccessRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setDecisionNote(trimToNull(req != null ? req.getDecisionNote() : null));
        accessRequestRepository.save(request);

        return ResponseEntity.ok(toView(request));
    }

    @PreAuthorize("@rbac.hasManagePermissionForAccessRequest(authentication, #requestId)")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> reject(
            Authentication authentication,
            @PathVariable Long requestId,
            @RequestBody(required = false) ReviewAccessRequestDto req
    ) {
        User reviewer = requireCurrentUser(authentication);
        AccessRequest request = requirePendingRequest(requestId);

        request.setStatus(AccessRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setDecisionNote(trimToNull(req != null ? req.getDecisionNote() : null));
        accessRequestRepository.save(request);

        return ResponseEntity.ok(toView(request));
    }

    private boolean hasManagePermissionForRequest(Authentication authentication, AccessRequest request) {
        if (authentication == null) {
            return false;
        }
        return bindingRepository.findByUserUsername(authentication.getName()).stream().anyMatch(binding ->
                binding.getProject().getId().equals(request.getProject().getId())
                        && binding.getEnvironment() == request.getEnvironment()
                        && binding.getRole().getPermissions().stream()
                        .anyMatch(permission -> "ACCESS_MANAGE".equals(permission.getCode().name()))
        );
    }

    private User requireCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Project requireProject(Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId is required");
        }
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Role requireRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private AccessRequest requirePendingRequest(Long requestId) {
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been reviewed");
        }
        return request;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AccessRequestView toView(AccessRequest request) {
        return new AccessRequestView(
                request.getId(),
                request.getRequester().getId(),
                request.getRequester().getUsername(),
                request.getRequester().getEmail(),
                request.getProject().getId(),
                request.getProject().getProjectKey(),
                request.getProject().getName(),
                request.getEnvironment().name(),
                request.getRequestedRole().getName(),
                request.getStatus().name(),
                request.getReason(),
                request.getReviewedBy() != null ? request.getReviewedBy().getUsername() : null,
                request.getDecisionNote(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    public record AccessRequestView(
            Long id,
            Long requesterId,
            String requesterUsername,
            String requesterEmail,
            Long projectId,
            String projectKey,
            String projectName,
            String environment,
            String roleName,
            String status,
            String reason,
            String reviewedByUsername,
            String decisionNote,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}
