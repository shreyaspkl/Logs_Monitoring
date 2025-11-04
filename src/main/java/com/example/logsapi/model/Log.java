package com.example.logsapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sourceApp;
    private String level;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    // getters & setters
    public String getLevel() {
        return level;
    }
    public String getSourceApp() {
        return sourceApp;
    }
    public String getMessage() {
        return message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
