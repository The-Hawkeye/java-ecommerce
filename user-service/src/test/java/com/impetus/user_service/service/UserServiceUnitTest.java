package com.impetus.user_service.service;

import com.impetus.user_service.dto.user.ChangePasswordRequest;
import com.impetus.user_service.dto.user.CreateUserRequest;
import com.impetus.user_service.dto.user.UpdateUserRequest;
import com.impetus.user_service.dto.user.UserResponse;
import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.RoleRepository;
import com.impetus.user_service.repository.UserRepository;
import com.impetus.user_service.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // -------- Helpers --------

    private User newUser(Long id, String email, String fullName, String phone, String encodedPassword, boolean active, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setPassword(encodedPassword);
        u.setIsActive(active);
        u.setRoles(Set.of(roles));
        return u;
    }

    private Role role(String name) {
        return new Role(name);
    }

    private CreateUserRequest createReq(String email, String password, String fullName, String phone) {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFullName(fullName);
        req.setPhone(phone);
        return req;
    }

    private UpdateUserRequest updateReq(String fullName, String phone) {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName(fullName);
        req.setPhone(phone);
        return req;
    }

    private ChangePasswordRequest changePwdReq(String current, String next) {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(current);
        req.setNewPassword(next);
        return req;
    }

    // ---------------- createUser ----------------


    @Test
    @DisplayName("createUser: success with existing USER role")
    void createUser_success_existingRole() {
        CreateUserRequest req = createReq("USER@Example.com", "plain", "John Doe", "9999999999");

        when(userRepository.findByEmail("USER@Example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain")).thenReturn("encoded-pass");

        Role existingRole = role("USER");
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(existingRole));

        User persisted = newUser(10L, "user@example.com", "John Doe", "9999999999", "encoded-pass", true, existingRole);
        when(userRepository.save(any(User.class))).thenReturn(persisted);

        // Act
        UserResponse resp = userService.createUser(req);

        // Assert response mapping
        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals("user@example.com", resp.getEmail()); // lowercased by service
        assertEquals("John Doe", resp.getFullName());
        assertEquals("9999999999", resp.getPhone());
        assertEquals(1, resp.getRoles().size());

        // Unwrap Optional or check membership
        // If roles in UserResponse are List<String>
        assertTrue(resp.getRoles().contains("USER"));
        // OR if you still want to use findFirst (order not guaranteed):
        assertEquals("USER", resp.getRoles().stream().findFirst().orElseThrow());

        // Verify saved user content
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("encoded-pass", saved.getPassword());
        assertEquals(1, saved.getRoles().size());

        // Check membership in Set<Role>
        assertTrue(saved.getRoles().stream().anyMatch(r -> "USER".equals(r.getRoleName())));
        // OR unwrap if you insist on findFirst (but beware of set iteration order):
        assertEquals("USER", saved.getRoles().stream()
                .findFirst()
                .map(Role::getRoleName)
                .orElseThrow());

        verify(roleRepository, never()).save(any(Role.class)); // role existed
    }


    @Test
    @DisplayName("createUser: success creates USER role when missing")
    void createUser_success_createsMissingRole() {
        CreateUserRequest req = createReq("new@ex.com", "pwd", "Alice", "8888888888");

        when(userRepository.findByEmail("new@ex.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("8888888888")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pwd")).thenReturn("enc");

        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0)); // return Role("USER")

        User persisted = newUser(11L, "new@ex.com", "Alice", "8888888888", "enc", true, role("USER"));
        when(userRepository.save(any(User.class))).thenReturn(persisted);

        UserResponse resp = userService.createUser(req);

        assertEquals(11L, resp.getId());
        assertEquals("new@ex.com", resp.getEmail());
        assertEquals(Set.of("USER"), resp.getRoles());

        verify(roleRepository).save(any(Role.class)); // created default role
    }

    @Test
    @DisplayName("createUser: throws IllegalArgumentException when email already exists")
    void createUser_emailExistsThrows() {
        CreateUserRequest req = createReq("dup@ex.com", "pwd", "Bob", "7777777777");

        when(userRepository.findByEmail("dup@ex.com")).thenReturn(Optional.of(newUser(1L, "dup@ex.com", "X", "Y", "Z", true)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(req));
        assertEquals("Email Already Exists", ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: throws IllegalArgumentException when phone already present")
    void createUser_phoneExistsThrows() {
        CreateUserRequest req = createReq("ok@ex.com", "pwd", "Bob", "7777777777");

        when(userRepository.findByEmail("ok@ex.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("7777777777")).thenReturn(Optional.of(newUser(2L, "other@ex.com", "Y", "7777777777", "Z", true)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(req));
        assertEquals("Phone Already Present", ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------------- getUser ----------------

    @Test
    @DisplayName("getUser: returns mapped UserResponse")
    void getUser_success() {
        User user = newUser(5L, "user@ex.com", "John", "9999999999", "enc", true, role("USER"), role("ADMIN"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        UserResponse resp = userService.getUser(5L);

        assertEquals(5L, resp.getId());
        assertEquals("user@ex.com", resp.getEmail());
        assertEquals("John", resp.getFullName());
        assertEquals("9999999999", resp.getPhone());
        assertEquals(Set.of("USER", "ADMIN"), resp.getRoles());
    }

    @Test
    @DisplayName("getUser: throws NoSuchElementException when not found")
    void getUser_notFoundThrows() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> userService.getUser(404L));
        assertEquals("User Not Found", ex.getMessage());
    }

    // ---------------- updateUser ----------------

    @Test
    @DisplayName("updateUser: updates fullName when provided")
    void updateUser_updatesFullName() {
        User existing = newUser(3L, "j@ex.com", "Old Name", "999", "enc", true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = updateReq("New Name", null);

        UserResponse resp = userService.updateUser(3L, req);

        assertEquals("New Name", resp.getFullName());
        verify(userRepository).save(userCaptor.capture());
        assertEquals("New Name", userCaptor.getValue().getFullName());
    }

    @Test
    @DisplayName("updateUser: updates phone when different and not taken")
    void updateUser_updatesPhone() {
        User existing = newUser(4L, "a@ex.com", "Alice", "111", "enc", true);
        when(userRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(userRepository.findByPhone("222")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = updateReq(null, "222");

        UserResponse resp = userService.updateUser(4L, req);

        assertEquals("222", resp.getPhone());
        verify(userRepository).save(userCaptor.capture());
        assertEquals("222", userCaptor.getValue().getPhone());
    }

    @Test
    @DisplayName("updateUser: throws NoSuchElementException when user not found")
    void updateUser_notFoundThrows() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateUserRequest req = updateReq("Name", "999");

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> userService.updateUser(999L, req));
        assertEquals("User not found", ex.getMessage());
    }

    // ---------------- changePassword ----------------

    @Test
    @DisplayName("changePassword: success encodes and saves new password")
    void changePassword_success() {
        User existing = newUser(8L, "c@ex.com", "Carol", "444", "enc-old", true);
        when(userRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("old", "enc-old")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("enc-new");

        ChangePasswordRequest req = changePwdReq("old", "new");

        userService.changePassword(8L, req);

        verify(userRepository).save(userCaptor.capture());
        assertEquals("enc-new", userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("changePassword: throws IllegalArgumentException when current password mismatch")
    void changePassword_wrongCurrentThrows() {
        User existing = newUser(9L, "d@ex.com", "Dave", "555", "enc-old", true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "enc-old")).thenReturn(false);

        ChangePasswordRequest req = changePwdReq("wrong", "new");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.changePassword(9L, req));
        assertEquals("Incorrect Current Password", ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword: throws NoSuchElementException when user not found")
    void changePassword_userNotFoundThrows() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        ChangePasswordRequest req = changePwdReq("old", "new");

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> userService.changePassword(404L, req));
        assertEquals("User not found", ex.getMessage());
    }

    // ---------------- listUsers ----------------

    @Test
    @DisplayName("listUsers: returns paginated mapped responses")
    void listUsers_success() {
        User u1 = newUser(1L, "u1@ex.com", "U1", "111", "p1", true, role("USER"));
        User u2 = newUser(2L, "u2@ex.com", "U2", "222", "p2", false, role("ADMIN"));

        Pageable pageable = PageRequest.of(0, 2);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(u1, u2), pageable, 2));

        List<UserResponse> responses = userService.listUsers(0, 2);

        assertEquals(2, responses.size());
        assertEquals("u1@ex.com", responses.get(0).getEmail());
        assertEquals(Set.of("USER"), responses.get(0).getRoles());
        assertEquals("u2@ex.com", responses.get(1).getEmail());
        assertEquals(Set.of("ADMIN"), responses.get(1).getRoles());

        // Ensure it's unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> responses.add(new UserResponse()));
    }

    // ---------------- deactivateUser / activateUser ----------------

    @Test
    @DisplayName("deactivateUser: sets isActive=false and saves")
    void deactivateUser_success() {
        User u = newUser(50L, "x@ex.com", "X", "000", "enc", true);
        when(userRepository.findById(50L)).thenReturn(Optional.of(u));

        userService.deactivateUser(50L, "reason");

        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().getIsActive());
    }

    @Test
    @DisplayName("deactivateUser: throws NoSuchElementException when not found")
    void deactivateUser_notFoundThrows() {
        when(userRepository.findById(51L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> userService.deactivateUser(51L, "reason"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    @DisplayName("activateUser: sets isActive=true and saves")
    void activateUser_success() {
        User u = newUser(60L, "y@ex.com", "Y", "111", "enc", false);
        when(userRepository.findById(60L)).thenReturn(Optional.of(u));

        userService.activateUser(60L);

        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getIsActive());
    }

    @Test
    @DisplayName("activateUser: throws NoSuchElementException when not found")
    void activateUser_notFoundThrows() {
        when(userRepository.findById(61L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> userService.activateUser(61L));
        assertEquals("User not found", ex.getMessage());
    }
}

