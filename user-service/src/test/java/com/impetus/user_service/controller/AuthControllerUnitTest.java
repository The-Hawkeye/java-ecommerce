package com.impetus.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.user_service.dto.auth.AuthResponse;
import com.impetus.user_service.dto.auth.LoginRequest;
import com.impetus.user_service.dto.auth.RefreshRequest;
import com.impetus.user_service.service.AuthService;
import com.impetus.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc tests for AuthController.
 * No Spring context; services are mocked manually.
 */
class AuthControllerUnitTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private UserService userService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        authService = Mockito.mock(AuthService.class);
        userService = Mockito.mock(UserService.class);

        AuthController controller = new AuthController(authService, userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // If you have a @ControllerAdvice, register here:
                // .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    // ---------- Helpers ----------

    private LoginRequest loginReq(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private RefreshRequest refreshReq(String token) {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(token);
        return req;
    }

    private AuthResponse authRes(String access, String refresh) {
        // Your service returns: new AuthResponse(accessToken, "Bearer", 60*60*12, refreshToken);
        return new AuthResponse(access, "Bearer", 60 * 60 * 12, refresh);
    }

    // ---------- POST /auth/login ----------

    @Test
    @DisplayName("POST /auth/login: 200 OK and ApiResponse with AuthResponse")
    void login_success() throws Exception {
        LoginRequest req = loginReq("user@example.com", "secret");
        AuthResponse serviceRes = authRes("access-123", "refresh-abc");

        when(authService.login(any(LoginRequest.class))).thenReturn(serviceRes);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Access Token generated successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("access-123"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-abc"));

        verify(authService).login(any(LoginRequest.class));
        verifyNoInteractions(userService);
    }

    // ---------- POST /auth/refresh ----------

    @Test
    @DisplayName("POST /auth/refresh: 200 OK and ApiResponse with new access token (no refresh token)")
    void refresh_success() throws Exception {
        RefreshRequest req = refreshReq("valid-refresh-xyz");
        // On refresh, your service returns AuthResponse(access, "Bearer", ..., null)
        AuthResponse serviceRes = authRes("new-access-999", null);

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(serviceRes);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Refresh token generated successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-999"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        verify(authService).refresh(any(RefreshRequest.class));
        verifyNoInteractions(userService);
    }

    // ---------- POST /auth/logout ----------

    @Test
    @DisplayName("POST /auth/logout: returns 200 OK (controller uses ResponseEntity.ok despite @ResponseStatus(NO_CONTENT))")
    void logout_success() throws Exception {
        RefreshRequest req = refreshReq("refresh-to-revoke");

        doNothing().when(authService).logout(any(RefreshRequest.class));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged Out Successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout(any(RefreshRequest.class));
        verifyNoInteractions(userService);
    }

    // ---------- POST /auth/validate ----------

    @Test
    @DisplayName("POST /auth/validate: accepts raw token in body, returns 200 OK with boolean data")
    void validate_success() throws Exception {
        String token = "some-jwt-token";
        when(authService.validate(eq(token))).thenReturn(true);

        // Since controller expects a raw String token in the body, send plain text
        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Request Authenticated Successfully"))
                .andExpect(jsonPath("$.data").value(true));

        verify(authService).validate(eq(token));
        verifyNoInteractions(userService);
    }

    // ---------- Optional: negative-path examples (uncomment if you register ControllerAdvice) ----------

    // @Test
    // @DisplayName("POST /auth/login: invalid credentials -> 400 (with ControllerAdvice mapping IllegalArgumentException)")
    // void login_invalidCredentials() throws Exception {
    //     when(authService.login(any(LoginRequest.class))).thenThrow(new IllegalArgumentException("Invalid credentials"));
    //
    //     mockMvc.perform(post("/auth/login")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(loginReq("user@example.com", "bad"))))
    //             .andExpect(status().isBadRequest());
    // }
}
