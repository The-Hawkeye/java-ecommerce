package com.impetus.user_service.service;

import com.impetus.user_service.dto.admin.CreateRoleRequest;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<String> listRoles();
    String createRole(CreateRoleRequest req);
    void assignRoles(Long userId, Set<String> roles);
    void revokeRoles(Long userId, Set<String> roles);
}
