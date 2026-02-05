package com.impetus.user_service.service.Impl;

import com.impetus.user_service.dto.admin.CreateRoleRequest;
import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.RoleRepository;
import com.impetus.user_service.repository.UserRepository;
import com.impetus.user_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private static final String DEFAULT_ROLE_NAME = "USER";

    @Override
    public List<String> listRoles() {
        return roleRepository.findAll().stream().map(Role::getRoleName).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String createRole(CreateRoleRequest req) {
        Role newRole = new Role(req.getRoleName());
        return roleRepository.save(newRole).getRoleName();
    }

//    @Override
//    public void assignRoles(Long userId, Set<String> roles) {
//        User u = userRepository.findById(userId).orElseThrow();
//        u.getRoles().clear();
//
//        for(String rname: roles){
//            Role r= roleRepository.findByRoleName(rname).orElseGet(()-> roleRepository.save(new Role(rname)));
//            u.getRoles().add(r);
//        }
//
//        userRepository.save(u);
//    }

    @Override
    @Transactional
    public void assignRoles(Long userId, Set<String> roles) {
        // Gracefully handle null/empty
        if (roles == null || roles.isEmpty()) {
            return;
        }

        // Normalize, trim, dedupe incoming names
        Set<String> requestedRoleNames = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedRoleNames.isEmpty()) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: "));

        Set<String> currentRoleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        Set<String> toAssignNames = requestedRoleNames.stream()
                .filter(rn -> !currentRoleNames.contains(rn))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (toAssignNames.isEmpty()) {
            return;
        }

        // Fetch existing roles for the to-assign names
        List<Role> existingRoles = roleRepository.findByRoleNameIn(toAssignNames);
        Set<String> existingRoleNames = existingRoles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        // Create missing roles (if any), preserving your current behavior
        Set<String> missingNames = toAssignNames.stream()
                .filter(rn -> !existingRoleNames.contains(rn))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Role> createdRoles = new ArrayList<>();
        for (String rn : missingNames) {
            createdRoles.add(roleRepository.save(new Role(rn)));
        }

        // Combine existing + newly created roles
        List<Role> rolesToAdd = new ArrayList<>(existingRoles);
        rolesToAdd.addAll(createdRoles);

        // Add only new ones (the Set prevents duplicates, but we also filtered by name)
        user.getRoles().addAll(rolesToAdd);

        // Persist
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void revokeRoles(Long userId, Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        // Normalize incoming role names (trim, dedupe)
        Set<String> requestedRoleNames = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedRoleNames.isEmpty()) {
            return;
        }

        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Fetch Role entities for the requested names
        List<Role> foundRoles = roleRepository.findByRoleNameIn(requestedRoleNames);
        Map<String, Role> foundByName = foundRoles.stream()
                .collect(Collectors.toMap(Role::getRoleName, Function.identity()));

        // Validate unknown role names (optional: you can ignore instead of throwing)
        Set<String> unknown = requestedRoleNames.stream()
                .filter(r -> !foundByName.containsKey(r))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown roles: " + unknown);
        }

        // Remove the roles
        boolean changed = user.getRoles().removeAll(foundRoles);

        // If everything was revoked (or user had none after removal), enforce default role
        if (user.getRoles().isEmpty()) {
            Role defaultRole = ensureDefaultRole(DEFAULT_ROLE_NAME);
            user.getRoles().add(defaultRole);
            changed = true; // a change happened
        }

        if (changed) {
            userRepository.save(user);
        }
    }

    private Role ensureDefaultRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
    }
}
