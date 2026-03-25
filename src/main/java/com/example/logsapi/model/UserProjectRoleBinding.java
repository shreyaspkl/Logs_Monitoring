package com.example.logsapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_project_role_bindings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_project_env_role",
                columnNames = {"user_id", "project_id", "environment", "role_id"}
        ))
public class UserProjectRoleBinding {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false) @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnvironmentType environment;

    @ManyToOne(optional = false) @JoinColumn(name = "role_id")
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public EnvironmentType getEnvironment() { return environment; }
    public void setEnvironment(EnvironmentType environment) { this.environment = environment; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}