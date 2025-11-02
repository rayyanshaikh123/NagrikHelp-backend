package com.nagrikHelp.config;

import com.nagrikHelp.model.Role;
import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return; // no seeding configured
        }
        String email = adminEmail.toLowerCase();
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            boolean changed = false;
            if (!adminName.equals(existing.getName())) {
                existing.setName(adminName);
                changed = true;
            }
            // Always reset role to SUPER_ADMIN
            if (existing.getRole() != Role.SUPER_ADMIN) {
                existing.setRole(Role.SUPER_ADMIN);
                changed = true;
            }
            // Reset password from properties on each run (dev convenience)
            String encoded = passwordEncoder.encode(adminPassword);
            if (!encoded.equals(existing.getPasswordHash())) {
                existing.setPasswordHash(encoded);
                changed = true;
            }
            if (changed) {
                userRepository.save(existing);
                log.info("Updated SUPER_ADMIN user: {}", email);
            } else {
                log.info("SUPER_ADMIN user already up-to-date: {}", email);
            }
        }, () -> {
            User admin = User.builder()
                    .name(adminName)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.SUPER_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded SUPER_ADMIN user: {}", email);
        });
    }
}
