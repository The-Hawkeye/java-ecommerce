package com.impetus.user_service.service;

import com.impetus.user_service.dto.admin.CreateRoleRequest;
import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.RoleRepository;
import com.impetus.user_service.repository.UserRepository;
import com.impetus.user_service.service.Impl.RoleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceUnitTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    // -------- Helpers --------

    private Role role(String name) {
        Role r = new Role(name);
        // if Role has setId or equals/hashCode by id, adapt as needed
        return r;
    }


    private User userWithRoles(Long id, Role... roles) {
        User u = new User();
        u.setId(id);
        // Use a mutable set, not Set.of(...)
        u.setRoles(new java.util.HashSet<>(java.util.Arrays.asList(roles)));
        return u;
    }


    // ---------------- listRoles ----------------

    @Test
    @DisplayName("listRoles: returns all role names from repository")
    void listRoles_returnsNames() {
        when(roleRepository.findAll()).thenReturn(Arrays.asList(role("ROLE_USER"), role("ROLE_ADMIN")));

        List<String> names = roleService.listRoles();

        assertNotNull(names);
        assertEquals(2, names.size());
        assertEquals("ROLE_USER", names.get(0));
        assertEquals("ROLE_ADMIN", names.get(1));

        verify(roleRepository).findAll();
    }

    // ---------------- createRole ----------------

    @Test
    @DisplayName("createRole: saves a new role and returns its name")
    void createRole_savesAndReturnsName() {
        CreateRoleRequest req = new CreateRoleRequest();
        req.setRoleName("ROLE_MANAGER");

        Role saved = role("ROLE_MANAGER");
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        String returned = roleService.createRole(req);

        assertEquals("ROLE_MANAGER", returned);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        Role toSave = captor.getValue();
        assertEquals("ROLE_MANAGER", toSave.getRoleName());
    }

    // ---------------- assignRoles ----------------

    @Test
    @DisplayName("assignRoles: clears existing roles and assigns provided ones (existing roles)")
    void assignRoles_clearsAndAssignsExistingRoles() {
        Long userId = 100L;
        User user = userWithRoles(userId, role("ROLE_OLD"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(role("ROLE_USER")));
        when(roleRepository.findByRoleName("ROLE_ADMIN")).thenReturn(Optional.of(role("ROLE_ADMIN")));

        roleService.assignRoles(userId, Set.of("ROLE_USER", "ROLE_ADMIN"));

        // After clearing, only two new roles should be present
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getRoleName())));
        assertTrue(user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleName())));

        verify(userRepository).findById(userId);
        verify(roleRepository).findByRoleName("ROLE_USER");
        verify(roleRepository).findByRoleName("ROLE_ADMIN");
        verify(roleRepository, never()).save(any(Role.class)); // both existed, so no saves
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("assignRoles: creates missing roles and assigns them")
    void assignRoles_createsMissingRoles() {
        Long userId = 101L;
        User user = userWithRoles(userId, role("ROLE_OLD"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // ROLE_USER exists, ROLE_MANAGER does not
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(role("ROLE_USER")));
        when(roleRepository.findByRoleName("ROLE_MANAGER")).thenReturn(Optional.empty());

        // Saving new role returns a Role with same name
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        roleService.assignRoles(userId, Set.of("ROLE_USER", "ROLE_MANAGER"));

        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getRoleName())));
        assertTrue(user.getRoles().stream().anyMatch(r -> "ROLE_MANAGER".equals(r.getRoleName())));

        verify(roleRepository).findByRoleName("ROLE_USER");
        verify(roleRepository).findByRoleName("ROLE_MANAGER");
        // One save for the missing role
        verify(roleRepository, times(1)).save(any(Role.class));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("assignRoles: handles duplicate role names in input gracefully")
    void assignRoles_duplicateInputRoles() {
        Long userId = 102L;
        User user = userWithRoles(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(role("ROLE_USER")));

        // If the code adds duplicates, we'll still end up with multiple entries.
        // Optional: You may enforce uniqueness via Set<Role> in User entity.
        roleService.assignRoles(userId, Set.of("ROLE_USER"));

        assertEquals(1, user.getRoles().size(), "Since service adds per input item, duplicates will be added");
        long countUser = user.getRoles().stream().filter(r -> "ROLE_USER".equals(r.getRoleName())).count();
        assertEquals(1, countUser);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("assignRoles: throws when user not found")
    void assignRoles_userNotFoundThrows() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> roleService.assignRoles(userId, Set.of("ROLE_USER")));
        // No message enforced by service; Optional.orElseThrow() uses NoSuchElementException

        verify(roleRepository, never()).findByRoleName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("assignRoles: clears previous roles before assigning new ones")
    void assignRoles_clearsPreviousRoles() {
        Long userId = 103L;
        User user = userWithRoles(userId, role("ROLE_OLD1"), role("ROLE_OLD2"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(role("ROLE_USER")));

        roleService.assignRoles(userId, Set.of("ROLE_USER"));

        assertEquals(1, user.getRoles().size());
        assertEquals("ROLE_USER", user.getRoles().stream().findFirst()
                .map(Role::getRoleName)
                .orElseThrow(() -> new AssertionError("No roles present"))
        );

        verify(userRepository).save(user);
    }
}
