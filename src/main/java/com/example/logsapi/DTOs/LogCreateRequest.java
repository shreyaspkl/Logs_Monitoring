package com.example.logsapi.DTOs;

import com.example.logsapi.model.EnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LogCreateRequest {
    @NotNull private Long projectId;
    @NotNull private EnvironmentType environment;
    private String appName;
    private String microservice;
    private String sourceApp;
    @NotBlank private String level;
    @NotBlank private String message;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public EnvironmentType getEnvironment() { return environment; }
    public void setEnvironment(EnvironmentType environment) { this.environment = environment; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getMicroservice() { return microservice; }
    public void setMicroservice(String microservice) { this.microservice = microservice; }
    public String getSourceApp() { return sourceApp; }
    public void setSourceApp(String sourceApp) { this.sourceApp = sourceApp; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}