package com.impetus.user_service.service.Impl;

import com.impetus.user_service.dto.user.ChangePasswordRequest;
import com.impetus.user_service.dto.user.CreateUserRequest;
import com.impetus.user_service.dto.user.UpdateUserRequest;
import com.impetus.user_service.dto.user.UserResponse;
import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.RoleRepository;
import com.impetus.user_service.repository.UserRepository;
import com.impetus.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {throw new IllegalArgumentException("Email Already Exists");});
        userRepository.findByPhone(req.getPhone()).ifPresent(user -> {throw new IllegalArgumentException("Phone Already Present");});

        User u = new User();
        u.setEmail(req.getEmail().toLowerCase(Locale.ROOT));
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setFullName(req.getFullName());
        u.setPhone(req.getPhone());

        Role userRole = roleRepository.findByRoleName("USER").orElseGet(()-> roleRepository.save(new Role("USER")));
        u.getRoles().add(userRole);

        User savedUser = userRepository.save(u);
        return mapToResponse(savedUser);
    }

    @Override
    public UserResponse getUser(Long id) {
        User u = userRepository.findById(id).orElseThrow(()-> new NoSuchElementException("User Not Found"));
        return mapToResponse(u);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User u = userRepository.findById(id).orElseThrow(()-> new NoSuchElementException("User not found"));
        if(req.getFullName() != null){
            u.setFullName(req.getFullName());
        }

        if(req.getPhone() != null && !req.getPhone().equals(u.getPhone())){
            userRepository.findByPhone(req.getPhone()).ifPresent(exist-> { throw new IllegalArgumentException("Phone ALready exists");});
            u.setPhone(req.getPhone());
        }
        User updatedUser = userRepository.save(u);
        return mapToResponse(updatedUser);

    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest req) {
        User u = userRepository.findById(id).orElseThrow(()-> new NoSuchElementException("User not found"));
        if(!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())){
            throw new IllegalArgumentException("Incorrect Current Password");
        }

        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
    }

    @Override
    public List<UserResponse> listUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).stream().map(this::mapToResponse).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void deactivateUser(Long id, String reason) {
        User u = userRepository.findById(id).orElseThrow(()-> new NoSuchElementException("User not found"));
        u.setIsActive(false);
        userRepository.save(u);
    }

    @Override
    public void activateUser(Long id) {
        User u = userRepository.findById(id).orElseThrow(()-> new NoSuchElementException("User not found"));
        u.setIsActive(true);
        userRepository.save(u);
    }

    private UserResponse mapToResponse(User u){
        Set<String> roles = u.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), roles);
    }
}
