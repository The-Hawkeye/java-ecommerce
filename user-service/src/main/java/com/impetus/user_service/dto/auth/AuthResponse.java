package com.impetus.user_service.dto.auth;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    String accessToken;
    String tokenType;
    long expiresInMs;
    String refreshToken;
}
