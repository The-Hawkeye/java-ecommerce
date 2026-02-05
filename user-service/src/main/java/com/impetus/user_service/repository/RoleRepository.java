package com.impetus.user_service.repository;

import com.impetus.user_service.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
    List<Role> findByRoleNameIn(Collection<String> roleNames);
}
