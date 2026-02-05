package com.impetus.user_service.controller;

import com.impetus.user_service.response.ApiResponse;
import com.impetus.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize(("hasRole('ADMIN')"))
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PutMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable("id") Long id, @RequestBody(required = false)Map<String, String> body){
        userService.deactivateUser(id, body == null ? null : body.get("reason"));
        return ResponseEntity.ok(new ApiResponse<>("User Deactivated Successfully", null));
    }

    @PutMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable("id") Long id){
        userService.activateUser(id);
        return ResponseEntity.ok(new ApiResponse<>("User activated", null));
    }

}
