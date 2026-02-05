package com.impetus.user_service.controller;

import com.impetus.user_service.dto.admin.AssignRoleRequest;
import com.impetus.user_service.dto.admin.CreateRoleRequest;
import com.impetus.user_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<?> listRoles(){
        return ResponseEntity.ok(roleService.listRoles());
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody CreateRoleRequest req){
        return ResponseEntity.status(201).body(roleService.createRole(req));
    }

    @PostMapping("/assign/{userId}")
    public ResponseEntity<Void> assignRoles(@PathVariable("userId") Long userId, @RequestBody AssignRoleRequest req){
        roleService.assignRoles(userId, req.getRoles());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke/{userId}")
    public ResponseEntity<Void> revokeRoles(@PathVariable("userId") Long userId, @RequestBody AssignRoleRequest req){
        roleService.revokeRoles(userId, req.getRoles());
        return ResponseEntity.noContent().build();
    }
}
