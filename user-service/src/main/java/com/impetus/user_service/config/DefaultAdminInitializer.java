package com.impetus.user_service.config;

import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.RoleRepository;
import com.impetus.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@RefreshScope
public class DefaultAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default.admin.email}")
    private String defaultAdminEmail;

    @Value("${default.admin.username}")
    private String defaultAdminUsername;

    @Value("${default.admin.password}")
    private String defaultAdminPassword;

    @Value("${default.admin.full-name:Default Admin}")
    private String defaultAdminFullName;

    public DefaultAdminInitializer(PasswordEncoder passwordEncoder,
                                   UserRepository userRepository,
                                   RoleRepository roleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = roleRepository.findByRoleName(ADMIN_ROLE)
                .orElseGet(() -> {
                    Role role = new Role(ADMIN_ROLE);
                    Role saved = roleRepository.save(role);
                    log.info("Created role: {}", ADMIN_ROLE);
                    return saved;
                });

        userRepository.findByEmail(defaultAdminEmail)
                .ifPresentOrElse(user -> {
                    // 3) Ensure the user has ADMIN role
                    if (user.getRoles() == null) {
                        user.setRoles(new HashSet<>());
                    }
                    boolean hasAdmin = user.getRoles().stream()
                            .anyMatch(r -> ADMIN_ROLE.equalsIgnoreCase(r.getRoleName()));
                    if (!hasAdmin) {
                        user.getRoles().add(adminRole);
                        userRepository.save(user);
                        log.info("Assigned ADMIN role to existing user: {}", user.getEmail());
                    } else {
                        log.info("Default admin already exists with ADMIN role: {}", user.getEmail());
                    }
                }, () -> {
                    // Create a new default admin user
                    User admin = new User();
                    admin.setFullName(defaultAdminFullName);
                    admin.setEmail(defaultAdminEmail);
                    admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                    admin.setIsActive(true);
                    admin.setIsEmailVerified(true); // optional: set as verified
                    admin.setPhone("9999999999");    // set a valid default or read from config

                    HashSet<Role> roles = new HashSet<>();
                    roles.add(adminRole);
                    admin.setRoles(roles);

                    userRepository.save(admin);
                    log.info("Default admin created: username='{}', email='{}'", defaultAdminUsername, defaultAdminEmail);
                });
    }
}
