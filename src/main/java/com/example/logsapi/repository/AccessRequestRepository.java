package com.example.logsapi.repository;

import com.example.logsapi.model.AccessRequest;
import com.example.logsapi.model.AccessRequestStatus;
import com.example.logsapi.model.EnvironmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<AccessRequest> findByRequesterUsernameOrderByCreatedAtDesc(String username);
    List<AccessRequest> findByStatusOrderByCreatedAtDesc(AccessRequestStatus status);
    List<AccessRequest> findByStatusAndProjectIdInOrderByCreatedAtDesc(AccessRequestStatus status, Collection<Long> projectIds);
    boolean existsByRequesterIdAndProjectIdAndEnvironmentAndRequestedRoleIdAndStatus(
            Long requesterId,
            Long projectId,
            EnvironmentType environment,
            Long requestedRoleId,
            AccessRequestStatus status
    );
}
