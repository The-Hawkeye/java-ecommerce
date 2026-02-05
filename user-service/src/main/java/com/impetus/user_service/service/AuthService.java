package com.impetus.user_service.service;

import com.impetus.user_service.dto.auth.AuthResponse;
import com.impetus.user_service.dto.auth.LoginRequest;
import com.impetus.user_service.dto.auth.RefreshRequest;

public interface AuthService {

    AuthResponse login(LoginRequest req);
    AuthResponse refresh(RefreshRequest req);
    void logout(RefreshRequest req);
    Boolean validate(String token);
}
