package com.example.logsapi.DTOs;

public class ProjectResponseDto {
    private Long id;
    private String projectKey;
    private String name;

    public ProjectResponseDto(Long id, String projectKey, String name) {
        this.id = id;
        this.projectKey = projectKey;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getName() {
        return name;
    }
}
