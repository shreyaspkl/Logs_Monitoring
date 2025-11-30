package com.example.logsapi.repository;

import com.example.logsapi.model.ProjectPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectPermissionRepository extends JpaRepository<ProjectPermission, Long> {
    boolean existsByUserIdAndProjectNameAndRole(Long userId, String projectName, String role);
    List<ProjectPermission> findByUserId(Long userId);
}
