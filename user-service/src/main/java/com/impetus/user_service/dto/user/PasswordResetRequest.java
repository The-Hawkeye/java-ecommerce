package com.impetus.user_service.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
    String emailOrPhone;
}
