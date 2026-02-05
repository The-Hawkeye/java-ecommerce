package com.impetus.user_service.controller;

import com.impetus.user_service.dto.user.ChangePasswordRequest;
import com.impetus.user_service.dto.user.CreateUserRequest;
import com.impetus.user_service.dto.user.UpdateUserRequest;
import com.impetus.user_service.dto.user.UserResponse;
import com.impetus.user_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    private Long getCurrentUserId(HttpServletRequest request){
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        if(userId == null){
            log.error("Gateway failed to assign X-User-Id header");
            throw new BadRequestException("Header missing");
        }
        log.info("UserId: "+userId);
        log.info("Roles from Header : "+ getCurrentUserRoles(request));
        return userId;
    }

    private String getCurrentUserRoles(HttpServletRequest request){
        String roles =  request.getHeader("X-User-Roles");
        if(roles == null){
            log.error("Gateway failed to assign X-User-Roles header");
            throw new BadRequestException("Header missing");
        }
        return roles;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest req){
        UserResponse res = userService.createUser(req);
        return ResponseEntity.created(URI.create("/user")).body(res);
    }

    @GetMapping()
    public ResponseEntity<UserResponse> getUser(HttpServletRequest request){
        Long id = getCurrentUserId(request);
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping()
    public ResponseEntity<UserResponse> updateUser(@RequestBody UpdateUserRequest req, HttpServletRequest request){
        Long id = getCurrentUserId(request);
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req, HttpServletRequest request){
        Long id = getCurrentUserId(request);
        userService.changePassword(id, req);
        return ResponseEntity.noContent().build();
    }

    //For admin only
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size){
        return ResponseEntity.ok(userService.listUsers(page, size));
    }
}
