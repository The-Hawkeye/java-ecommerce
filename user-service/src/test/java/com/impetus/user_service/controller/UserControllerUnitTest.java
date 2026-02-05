package com.impetus.user_service.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.user_service.dto.user.ChangePasswordRequest;
import com.impetus.user_service.dto.user.CreateUserRequest;
import com.impetus.user_service.dto.user.UpdateUserRequest;
import com.impetus.user_service.dto.user.UserResponse;
import com.impetus.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc unit tests for UserController.
 * No Spring context; UserService is mocked manually.
 *
 * Security note: @PreAuthorize(...) is NOT enforced in standalone setup.
 * To test security, use @WebMvcTest with Spring Security test support.
 */
class UserControllerUnitTest {

    private MockMvc mockMvc;
    private UserService userService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // If you have a @ControllerAdvice for error handling, add it here:
                // .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    // ---------- Helpers ----------

    private CreateUserRequest createReq(String fullName, String email, String phone, String password) {
        CreateUserRequest req = new CreateUserRequest();
        req.setFullName(fullName);
        req.setEmail(email);
        req.setPhone(phone);
        req.setPassword(password);
        return req;
    }

    /** Update request uses fullName + phone */
    private UpdateUserRequest updateReq(String fullName, String phone) {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName(fullName);
        req.setPhone(phone);
        return req;
    }

    private ChangePasswordRequest changePwdReq(String oldPwd, String newPwd) {
        ChangePasswordRequest req = new ChangePasswordRequest();
//        req.setOldPassword(oldPwd);
        req.setNewPassword(newPwd);
        return req;
    }

    private UserResponse userRes(Long id, String fullName, String email, String phone,
                                 boolean isActive, boolean isEmailVerified) {
        UserResponse res = new UserResponse();
        res.setId(id);
        res.setFullName(fullName);
        res.setEmail(email);
        res.setPhone(phone);
        return res;
    }

    // ---------- POST /user ----------

    @Test
    @DisplayName("POST /user: returns 201 Created with JSON body and Location header")
    void createUser_success() throws Exception {
        CreateUserRequest req = createReq("Anubhav Maurya", "anubhav@example.com", "9876543210", "Secret@123");
        UserResponse created = userRes(100L, "Anubhav Maurya", "anubhav@example.com", "9876543210", true, false);

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", URI.create("/users").toString()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.fullName").value("Anubhav Maurya"))
                .andExpect(jsonPath("$.email").value("anubhav@example.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    // ---------- GET /user/{id} ----------

    @Test
    @DisplayName("GET /user/{id}: returns 200 OK with JSON body")
    void getUser_success() throws Exception {
        Long id = 101L;
        UserResponse res = userRes(id, "Alice Doe", "alice@example.com", "9876501234", true, true);

        when(userService.getUser(eq(id))).thenReturn(res);

        mockMvc.perform(get("/user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.fullName").value("Alice Doe"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.phone").value("9876501234"));

        verify(userService).getUser(eq(id));
    }

    // ---------- PUT /user/{id} ----------

    @Test
    @DisplayName("PUT /user/{id}: returns 200 OK with updated JSON body")
    void updateUser_success() throws Exception {
        Long id = 102L;

        // Send fullName and phone in request payload
        UpdateUserRequest req = updateReq("Bob Marley", "9876543211");

        // Service returns updated UserResponse
        UserResponse updated = userRes(id, "Bob Marley", "bob@newmail.com", "9876543211", true, false);

        when(userService.updateUser(eq(id), any(UpdateUserRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/user/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.fullName").value("Bob Marley"))
                .andExpect(jsonPath("$.email").value("bob@newmail.com"))
                .andExpect(jsonPath("$.phone").value("9876543211"));

        verify(userService).updateUser(eq(id), any(UpdateUserRequest.class));
    }

    // ---------- POST /user/{id}/change-password ----------

    @Test
    @DisplayName("POST /user/{id}/change-password: returns 204 No Content")
    void changePassword_success() throws Exception {
        Long id = 103L;
        ChangePasswordRequest req = changePwdReq("oldPass123", "newPass456");

        doNothing().when(userService).changePassword(eq(id), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/user/{id}/change-password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq(id), any(ChangePasswordRequest.class));
    }

    // ---------- GET /user (list) ----------

    @Test
    @DisplayName("GET /user: returns 200 OK with list of users (JSON array) with pagination params")
    void listUsers_success() throws Exception {
        int page = 1;
        int size = 5;

        List<UserResponse> users = List.of(
                userRes(201L, "Eve Adams", "eve@example.com", "9876512345", true, false),
                userRes(202L, "Mark Twin", "mark@example.com", "9876523456", false, true)
        );

        when(userService.listUsers(eq(page), eq(size))).thenReturn(users);

        mockMvc.perform(get("/user")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(201))
                .andExpect(jsonPath("$[0].fullName").value("Eve Adams"))
                .andExpect(jsonPath("$[0].email").value("eve@example.com"))
                .andExpect(jsonPath("$[0].phone").value("9876512345"))
                .andExpect(jsonPath("$[1].id").value(202))
                .andExpect(jsonPath("$[1].fullName").value("Mark Twin"))
                .andExpect(jsonPath("$[1].email").value("mark@example.com"))
                .andExpect(jsonPath("$[1].phone").value("9876523456"));

        verify(userService).listUsers(eq(page), eq(size));
    }

    // ---------- Optional negative cases (if you add validation / advice) ----------

//     @Test
//     @DisplayName("POST /user: missing required fields -> 400 (if you add validation)")
//     void createUser_missingFields() throws Exception {
//         mockMvc.perform(post("/user")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content("{}"))
//                 .andExpect(status().isBadRequest());
//         verify(userService, never()).createUser(any());
//     }

     @Test
     @DisplayName("GET /user: default pagination params used if not provided")
     void listUsers_defaultPaging() throws Exception {
         when(userService.listUsers(eq(0), eq(20))).thenReturn(List.of());

         mockMvc.perform(get("/user"))
                 .andExpect(status().isOk());

         verify(userService).listUsers(eq(0), eq(20));
     }
}