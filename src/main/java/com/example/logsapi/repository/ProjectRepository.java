package com.example.logsapi.repository;

import com.example.logsapi.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectKey(String projectKey);
}