package com.impetus.user_service.controller;

import com.impetus.user_service.dto.auth.AuthResponse;
import com.impetus.user_service.dto.auth.LoginRequest;
import com.impetus.user_service.dto.auth.RefreshRequest;
import com.impetus.user_service.response.ApiResponse;
import com.impetus.user_service.service.AuthService;
import com.impetus.user_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    private ResponseCookie buildRefreshCookie(String token, boolean expireNow) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", token == null ? "" : token)
                .httpOnly(true)
                .secure(true) // required for SameSite=None; ensure HTTPS in prod
                .path("/auth/refresh") // cookie is only sent to this route
                .sameSite("None")      // required for cross-site cookies
                .maxAge(expireNow ? Duration.ofSeconds(0)  : Duration.ofDays(7));
        return builder.build();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest req, HttpServletResponse response) {
        AuthResponse res = authService.login(req);
        // Set refresh token cookie (HttpOnly, Secure, path-restricted)
        ResponseCookie refreshCookie = buildRefreshCookie(res.getRefreshToken(), false);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        // Optionally do NOT include refresh token in body
        res.setRefreshToken(null);
        return ResponseEntity.ok(new ApiResponse<>("Access Token generated successfully", res));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>("Missing refresh token cookie", null));
        }
        // Refresh & rotate token
        AuthResponse res = authService.refresh(new RefreshRequest(refreshToken));

        // Reset cookie with the new rotated refresh token
        ResponseCookie refreshCookie = buildRefreshCookie(res.getRefreshToken(), false);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        res.setRefreshToken(null);
        return ResponseEntity.ok(new ApiResponse<>("Refresh token generated successfully", res));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            // Invalidate server-side refresh token (DB/Redis/blacklist)
            authService.logout(new RefreshRequest(refreshToken));
        }

        // Clear cookie by setting Max-Age=0 with the same attributes
        ResponseCookie clearCookie = buildRefreshCookie(null, true);
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(new ApiResponse<>("Logged Out Successfully", null));
    }

}
