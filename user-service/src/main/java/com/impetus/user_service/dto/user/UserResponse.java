package com.impetus.user_service.dto.user;

import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    Long id;
    String email;
    String fullName;
    String phone;
    Set<String> roles;
}
