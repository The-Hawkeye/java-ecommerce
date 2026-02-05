package com.impetus.user_service.service;

import com.impetus.user_service.config.JwtTokenProvider;
import com.impetus.user_service.dto.auth.AuthResponse;
import com.impetus.user_service.dto.auth.LoginRequest;
import com.impetus.user_service.dto.auth.RefreshRequest;
import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.UserRepository;
import com.impetus.user_service.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    // -------- Helpers --------

    private User activeUser(Long id, String email, String encodedPassword, Set<Role> roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword(encodedPassword);
        u.setIsActive(true);
        u.setRoles(roles);
        return u;
    }

    private User inactiveUser(Long id, String email, String encodedPassword, Set<Role> roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword(encodedPassword);
        u.setIsActive(false);
        u.setRoles(roles);
        return u;
    }

    private Role role(String name) {
        Role r = new Role();
        r.setRoleName(name);
        return r;
    }

    // ---------------- login ----------------

    @Test
    @DisplayName("login: success issues access and refresh tokens")
    void login_success() {
        String email = "USER@EXAMPLE.COM";
        String normalized = email.toLowerCase(Locale.ROOT);
        String rawPassword = "plain";
        String encodedPassword = "encoded";
        Long userId = 42L;

        Set<Role> roles = Set.of(role("ROLE_USER"), role("ROLE_ADMIN"));
        User user = activeUser(userId, normalized, encodedPassword, roles);

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(rawPassword);

        when(userRepository.findByEmail(normalized)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(String.valueOf(userId), Set.of("ROLE_USER", "ROLE_ADMIN"), "auth-key-v1"))
                .thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(String.valueOf(userId), "auth-key-v1"))
                .thenReturn("refresh-token");

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("access-token", resp.getAccessToken());
        assertEquals("Bearer", resp.getTokenType());
        assertEquals("refresh-token", resp.getRefreshToken());

        verify(userRepository).findByEmail(normalized);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(jwtTokenProvider).createAccessToken(String.valueOf(userId), Set.of("ROLE_USER", "ROLE_ADMIN"), "auth-key-v1");
        verify(jwtTokenProvider).createRefreshToken(String.valueOf(userId), "auth-key-v1");
    }

    @Test
    @DisplayName("login: throws IllegalArgumentException when user not found by email")
    void login_userNotFoundThrows() {
        String email = "missing@example.com";
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("whatever");

        when(userRepository.findByEmail(email.toLowerCase(Locale.ROOT))).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(req));
        assertEquals("Invalid Credentials", ex.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList(), anyString());
        verify(jwtTokenProvider, never()).createRefreshToken(anyString(), anyString());
    }

    @Test
    @DisplayName("login: throws SecurityException when user is disabled")
    void login_userDisabledThrows() {
        String email = "inactive@example.com";
        String normalized = email.toLowerCase(Locale.ROOT);

        User user = inactiveUser(100L, normalized, "encoded", Set.of(role("ROLE_USER")));

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("plain");

        when(userRepository.findByEmail(normalized)).thenReturn(Optional.of(user));

        SecurityException ex = assertThrows(SecurityException.class, () -> authService.login(req));
        assertEquals("User Disabled", ex.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList(), anyString());
        verify(jwtTokenProvider, never()).createRefreshToken(anyString(), anyString());
    }

    @Test
    @DisplayName("login: throws IllegalArgumentException when password does not match")
    void login_wrongPasswordThrows() {
        String email = "user@example.com";
        String normalized = email.toLowerCase(Locale.ROOT);
        User user = activeUser(7L, normalized, "encoded", Set.of(role("ROLE_USER")));

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("wrong");

        when(userRepository.findByEmail(normalized)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(req));
        assertEquals("Invalid credentials", ex.getMessage());

        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList(), anyString());
        verify(jwtTokenProvider, never()).createRefreshToken(anyString(), anyString());
    }

    // ---------------- refresh ----------------

//    @Test
//    @DisplayName("refresh: success when refresh token typ is 'refresh'")
//    @SuppressWarnings("unchecked")
//    void refresh_success() {
//        String refreshToken = "valid-refresh";
//        Long userId = 55L;
//
//        // Mock JWS and Claims
//        Jws<Claims> jws = mock(Jws.class);
//        Claims claims = mock(Claims.class);
//
//        when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(jws);
//        when(jws.getBody()).thenReturn(claims);
//
//        when(claims.get("typ")).thenReturn("refresh");
//        when(claims.getSubject()).thenReturn(String.valueOf(userId));
//
//        User user = activeUser(userId, "user@example.com", "encoded", Set.of(role("ROLE_USER")));
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//
//        when(jwtTokenProvider.createAccessToken(String.valueOf(userId), Set.of("ROLE_USER"), "auth-key-v1"))
//                .thenReturn("new-access");
//
//        RefreshRequest req = new RefreshRequest();
//        req.setRefreshToken(refreshToken);
//
//        AuthResponse resp = authService.refresh(req);
//
//        assertNotNull(resp);
//        assertEquals("new-access", resp.getAccessToken());
//        assertEquals("Bearer", resp.getTokenType());
//        assertNull(resp.getRefreshToken(), "Refresh flow should not return a new refresh token");
//
//        verify(jwtTokenProvider).parseToken(refreshToken);
//        verify(userRepository).findById(userId);
//        verify(jwtTokenProvider).createAccessToken(String.valueOf(userId), Set.of("ROLE_USER"), "auth-key-v1");
//    }

//    @Test
//    @DisplayName("refresh: throws IllegalArgumentException when typ is missing or not 'refresh'")
//    @SuppressWarnings("unchecked")
//    void refresh_invalidTypThrows() {
//        String refreshToken = "invalid-typ";
//        Jws<Claims> jws = mock(Jws.class);
//        Claims claims = mock(Claims.class);
//
//        when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(jws);
//        when(jws.getBody()).thenReturn(claims);
//
//        when(claims.get("typ")).thenReturn("access"); // wrong type
//
//        RefreshRequest req = new RefreshRequest();
//        req.setRefreshToken(refreshToken);
//
//        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.refresh(req));
//        assertEquals("Invalid refresh Token", ex.getMessage()); // caught and rethrown in catch-block
//
//        verify(userRepository, never()).findById(anyLong());
//        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList(), anyString());
//    }

    @Test
    @DisplayName("refresh: throws IllegalArgumentException when parse fails")
    void refresh_parseFailureThrows() {
        String refreshToken = "bad-token";
        when(jwtTokenProvider.parseToken(refreshToken)).thenThrow(new RuntimeException("parse error"));

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(refreshToken);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.refresh(req));
        assertEquals("Invalid refresh Token", ex.getMessage());

        verify(userRepository, never()).findById(anyLong());
        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList(), anyString());
    }

    // ---------------- validate ----------------

    @Test
    @DisplayName("validate: delegates to jwtTokenProvider.validate")
    void validate_delegates() {
        String token = "some-token";
        when(jwtTokenProvider.validate(token)).thenReturn(true);

        Boolean result = authService.validate(token);

        assertTrue(result);
        verify(jwtTokenProvider).validate(token);
    }

    // ---------------- logout (no-op currently) ----------------

    @Test
    @DisplayName("logout: current implementation is no-op (should not throw)")
    void logout_noop() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("anything");
        assertDoesNotThrow(() -> authService.logout(req));
    }
}
