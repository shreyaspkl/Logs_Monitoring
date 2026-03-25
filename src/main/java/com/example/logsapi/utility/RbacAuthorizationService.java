package com.example.logsapi.utility;

import com.example.logsapi.model.PermissionCode;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("rbac")
public class RbacAuthorizationService {

    private final UserProjectRoleBindingRepository bindingRepo;

    public RbacAuthorizationService(UserProjectRoleBindingRepository bindingRepo) {
        this.bindingRepo = bindingRepo;
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
}