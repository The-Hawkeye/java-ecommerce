package com.impetus.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.user_service.dto.admin.CreateRoleRequest;
import com.impetus.user_service.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc unit tests for RoleController.
 * No Spring context; RoleService is mocked manually.
 *
 * Security note: @PreAuthorize("hasRole('ADMIN')") is not enforced in standalone setup.
 * If you want to test security, switch to @WebMvcTest with Spring Security test support.
 */
class RoleControllerUnitTest {

    private MockMvc mockMvc;
    private RoleService roleService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        roleService = Mockito.mock(RoleService.class);
        RoleController controller = new RoleController(roleService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // If you have a @ControllerAdvice for error handling, add it here:
                // .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    // ---------- Helpers ----------

    private CreateRoleRequest createReq(String name) {
        CreateRoleRequest req = new CreateRoleRequest();
        req.setRoleName(name);
        return req;
    }

    private String rolesJson(String... roles) throws Exception {
        // Creates JSON payload: {"roles":["ROLE_USER","ROLE_ADMIN"]}
        return objectMapper.writeValueAsString(Map.of("roles", List.of(roles)));
    }

    // ---------- GET /admin/roles ----------

    @Test
    @DisplayName("GET /admin/roles: returns 200 OK with list of role names")
    void listRoles_success() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));

        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$[1]").value("ROLE_ADMIN"));

        verify(roleService).listRoles();
    }

    // ---------- POST /admin/roles ----------


    @Test
    @DisplayName("POST /admin/roles: returns 201 Created with created role name in body (text/plain)")
    void createRole_success() throws Exception {
        when(roleService.createRole(any(CreateRoleRequest.class))).thenReturn("ROLE_MANAGER");

        mockMvc.perform(post("/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq("ROLE_MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("ROLE_MANAGER")); // plain text, no JSON quotes

        verify(roleService).createRole(any(CreateRoleRequest.class));
    }


    // ---------- POST /admin/roles/assign/{userId} ----------
    // Updated: Controller now uses @RequestBody AssignRoleRequest req (JSON)

    @Test
    @DisplayName("POST /admin/roles/assign/{userId}: returns 204 No Content and calls service with provided roles (JSON body)")
    void assignRoles_success() throws Exception {
        Long userId = 100L;

        mockMvc.perform(post("/admin/roles/assign/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolesJson("ROLE_USER", "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        // Verify service call with Set<String> containing both roles
        verify(roleService).assignRoles(eq(userId), eq(Set.of("ROLE_USER", "ROLE_ADMIN")));
    }

    // ---------- POST /admin/roles/revoke/{userId} ----------
    // Updated: Controller now uses @RequestBody AssignRoleRequest req (JSON)

    @Test
    @DisplayName("POST /admin/roles/revoke/{userId}: returns 204 No Content and calls service with provided roles (JSON body)")
    void revokeRoles_success() throws Exception {
        Long userId = 101L;

        mockMvc.perform(post("/admin/roles/revoke/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolesJson("ROLE_MANAGER")))
                .andExpect(status().isNoContent());

        verify(roleService).revokeRoles(eq(userId), eq(Set.of("ROLE_MANAGER")));
    }

    // ---------- Optional negative cases (if you add validation / advice) ----------

    // @Test
    // @DisplayName("POST /admin/roles: missing roleName -> 400 (if you add validation)")
    // void createRole_missingName() throws Exception {
    //     mockMvc.perform(post("/admin/roles")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content("{}"))
    //             .andExpect(status().isBadRequest());
    //     verify(roleService, never()).createRole(any());
    // }

    // @Test
    // @DisplayName("POST /admin/roles/assign/{userId}: missing roles -> 400 (if you add validation)")
    // void assignRoles_missingRoles() throws Exception {
    //     mockMvc.perform(post("/admin/roles/assign/{userId}", 1L)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content("{}"))
    //             .andExpect(status().isBadRequest());
    //     verify(roleService, never()).assignRoles(anyLong(), anySet());
    // }
}

