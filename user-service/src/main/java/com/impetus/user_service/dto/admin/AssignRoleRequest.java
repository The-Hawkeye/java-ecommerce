package com.impetus.user_service.dto.admin;

import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssignRoleRequest {
    Set<String> roles;
}
