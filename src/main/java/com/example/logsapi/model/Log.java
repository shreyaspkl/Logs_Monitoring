package com.example.logsapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // explicit column mapping to avoid any naming strategy mismatch
    @Column(name = "project_name")
    private String projectName;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "microservice")
    private String microservice;

    @Column(name = "source_app")
    private String sourceApp;

    private String level;

    @Column(columnDefinition = "text")
    private String message;

    private LocalDateTime timestamp = LocalDateTime.now();

    public Log() {}

    // ------------------ GETTERS ------------------
    public Long getId() { return id; }

    public String getProjectName() { return projectName; }
    public String getAppName() { return appName; }
    public String getMicroservice() { return microservice; }
    public String getSourceApp() { return sourceApp; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // ------------------ SETTERS ------------------
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setAppName(String appName) { this.appName = appName; }
    public void setMicroservice(String microservice) { this.microservice = microservice; }
    public void setSourceApp(String sourceApp) { this.sourceApp = sourceApp; }
    public void setLevel(String level) { this.level = level; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Log{" +
                "id=" + id +
                ", projectName='" + projectName + '\'' +
                ", appName='" + appName + '\'' +
                ", microservice='" + microservice + '\'' +
                ", sourceApp='" + sourceApp + '\'' +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
