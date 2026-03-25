package com.example.logsapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
})
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PermissionCode code;

    @Column(nullable = false, length = 120)
    private String description;

    // getters/setters
    public Long getId() { return id; }
    public PermissionCode getCode() { return code; }
    public void setCode(PermissionCode code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}