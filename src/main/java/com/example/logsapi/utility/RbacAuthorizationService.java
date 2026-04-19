package com.example.logsapi.utility;

import com.example.logsapi.model.AccessRequest;
import com.example.logsapi.model.PermissionCode;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.AccessRequestRepository;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("rbac")
public class RbacAuthorizationService {

    private final UserProjectRoleBindingRepository bindingRepo;
    private final AccessRequestRepository accessRequestRepository;

    public RbacAuthorizationService(UserProjectRoleBindingRepository bindingRepo,
                                    AccessRequestRepository accessRequestRepository) {
        this.bindingRepo = bindingRepo;
        this.accessRequestRepository = accessRequestRepository;
    }

    public boolean hasPermission(Authentication auth, Long projectId, String env, String permissionCode) {
        if (auth == null || auth.getName() == null) return false;
        PermissionCode required = PermissionCode.valueOf(permissionCode);

        List<UserProjectRoleBinding> bindings = bindingRepo.findByUserUsername(auth.getName());
        return bindings.stream().anyMatch(b ->
                b.getProject().getId().equals(projectId)
                        && b.getEnvironment().name().equalsIgnoreCase(env)
                        && b.getRole().getPermissions().stream().anyMatch(p -> p.getCode() == required)
        );
    }

    public List<UserProjectRoleBinding> getBindings(Authentication auth) {
        if (auth == null || auth.getName() == null) return List.of();
        return bindingRepo.findByUserUsername(auth.getName());
    }

    public boolean hasManagePermissionForAccessRequest(Authentication auth, Long requestId) {
        if (auth == null || auth.getName() == null || requestId == null) {
            return false;
        }

        AccessRequest request = accessRequestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return false;
        }

        return hasPermission(auth, request.getProject().getId(), request.getEnvironment().name(), "ACCESS_MANAGE");
    }
}
