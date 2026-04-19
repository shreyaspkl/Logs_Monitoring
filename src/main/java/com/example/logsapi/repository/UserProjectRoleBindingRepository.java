package com.example.logsapi.repository;

import com.example.logsapi.model.UserProjectRoleBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProjectRoleBindingRepository extends JpaRepository<UserProjectRoleBinding, Long> {
    List<UserProjectRoleBinding> findByUserUsername(String username);
    List<UserProjectRoleBinding> findByUserId(Long userId);
    boolean existsByUserIdAndProjectIdAndEnvironmentAndRoleId(Long userId,
                                                              Long projectId,
                                                              com.example.logsapi.model.EnvironmentType environment,
                                                              Long roleId);
    long deleteByUserIdAndProjectIdAndEnvironmentAndRoleId(Long userId, Long projectId,
                                                           com.example.logsapi.model.EnvironmentType environment,
                                                           Long roleId);
}