package com.example.logsapi.repository;

import com.example.logsapi.model.Permission;
import com.example.logsapi.model.PermissionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(PermissionCode code);
}