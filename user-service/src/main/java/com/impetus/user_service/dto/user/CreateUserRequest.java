package com.impetus.user_service.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserRequest {
    String email;
    String password;
    String fullName;
    String phone;
}
