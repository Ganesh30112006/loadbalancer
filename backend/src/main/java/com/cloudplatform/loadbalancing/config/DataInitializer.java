package com.cloudplatform.loadbalancing.config;

import com.cloudplatform.loadbalancing.entity.User;
import com.cloudplatform.loadbalancing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDefaultAdminUser();
    }

    private void createDefaultAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            log.info("Default admin user already exists");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .email("admin@cloudcontrolplane.io")
                .passwordHash(passwordEncoder.encode("admin123"))
                .firstName("System")
                .lastName("Administrator")
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Created default admin user: admin / admin123");
        log.info("================================================");
        log.info("  DEFAULT ADMIN CREDENTIALS");
        log.info("  Username: admin");
        log.info("  Password: admin123");
        log.info("  Email: admin@cloudcontrolplane.io");
        log.info("  CHANGE THIS IN PRODUCTION!");
        log.info("================================================");
    }
}
