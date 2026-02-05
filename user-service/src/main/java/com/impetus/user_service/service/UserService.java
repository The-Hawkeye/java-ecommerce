package com.impetus.user_service.service;

import com.impetus.user_service.dto.user.ChangePasswordRequest;
import com.impetus.user_service.dto.user.CreateUserRequest;
import com.impetus.user_service.dto.user.UpdateUserRequest;
import com.impetus.user_service.dto.user.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest req);
    UserResponse getUser(Long id);
    UserResponse updateUser(Long id, UpdateUserRequest req);
    void changePassword(Long id, ChangePasswordRequest req);
    List<UserResponse> listUsers(int page, int size);
    void deactivateUser(Long id, String reason);
    void activateUser(Long id);
}
