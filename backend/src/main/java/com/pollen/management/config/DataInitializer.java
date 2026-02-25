package com.pollen.management.config;

import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 系统启动时初始化默认账户
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "admin123";

    @Override
    public void run(String... args) {
        createDefaultUserIfNotExists("admin", Role.ADMIN);
        createDefaultUserIfNotExists("leader", Role.LEADER);
        createDefaultUserIfNotExists("teacher", Role.VICE_LEADER);
        createDefaultUserIfNotExists("intern", Role.INTERN);
    }

    private void createDefaultUserIfNotExists(String username, Role role) {
        if (userRepository.existsByUsername(username)) {
            log.info("默认账户已存在，跳过创建: {}", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("默认账户创建成功: {} ({})", username, role);
    }
}
