package com.impetus.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc unit tests for AdminController.
 * No Spring context; dependencies are mocked manually.
 */
class AdminControllerUnitTest {

    private MockMvc mockMvc;
    private UserService userService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        userService = Mockito.mock(UserService.class);
        AdminController controller = new AdminController(userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // If you have a @ControllerAdvice for error shaping, you can register it here:
                // .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    // ------------------- deactivate -------------------

    @Test
    @DisplayName("PUT /admin/users/{id}/deactivate: with reason -> 200 OK & message")
    void deactivate_withReason_success() throws Exception {
        Long userId = 123L;
        Map<String, String> body = Map.of("reason", "Policy violation");

        mockMvc.perform(put("/admin/users/{id}/deactivate", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()) // Note: ResponseEntity.ok overrides @ResponseStatus(NO_CONTENT)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User Deactivated Successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).deactivateUser(eq(userId), eq("Policy violation"));
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/deactivate: without body -> 200 OK & null reason")
    void deactivate_noBody_success() throws Exception {
        Long userId = 124L;

        mockMvc.perform(put("/admin/users/{id}/deactivate", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User Deactivated Successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).deactivateUser(eq(userId), eq(null)); // controller passes null when body is missing
    }

    // ------------------- activate -------------------

    @Test
    @DisplayName("PUT /admin/users/{id}/activate: -> 200 OK & message")
    void activate_success() throws Exception {
        Long userId = 200L;

        mockMvc.perform(put("/admin/users/{id}/activate", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User activated"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).activateUser(eq(userId));
    }

    // ------------------- edge cases & notes -------------------
    // If you add validation or error handling, add tests like:
    // - when userService throws NoSuchElementException -> expect 404 (with @ControllerAdvice)
    // - when userService throws SecurityException -> expect 403 (with @ControllerAdvice)
    // For PreAuthorize('hasRole('ADMIN')), consider Spring Security test setup (MockMvc with filters).
}

