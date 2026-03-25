package com.example.logsapi.utility;

import com.example.logsapi.model.EnvironmentType;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessAdminService {

    private final UserProjectRoleBindingRepository bindingRepo;

    public AccessAdminService(UserProjectRoleBindingRepository bindingRepo) {
        this.bindingRepo = bindingRepo;
    }

    @Transactional
    public long revoke(Long userId, Long projectId, EnvironmentType environment, Long roleId) {
        return bindingRepo.deleteByUserIdAndProjectIdAndEnvironmentAndRoleId(
                userId, projectId, environment, roleId
        );
    }
}