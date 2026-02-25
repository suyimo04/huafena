package com.pollen.management.service;

import com.pollen.management.dto.RegisterRequest;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 1: 新用户不变量
 * **Validates: Requirements 1.1, 1.2, 4.1, 4.2**
 *
 * For any 通过注册页面或公开问卷链接创建的用户，该用户的角色应为 APPLICANT，
 * 账户状态应为禁用（enabled=false）。
 */
class NewUserInvariantProperties {

    @Property(tries = 100)
    void registeredUserHasApplicantRoleAndIsDisabled(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String username,
            @ForAll @StringLength(min = 6, max = 100) String password) {

        // Arrange: mock dependencies (same pattern as AuthServiceImplTest)
        UserRepository userRepository = mock(UserRepository.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthServiceImpl authService = new AuthServiceImpl(userRepository, applicationRepository, passwordEncoder);
        RegisterRequest request = new RegisterRequest(username, password, Map.of("q1", "a1"));

        // Act
        authService.register(request);

        // Assert: saved user has role=APPLICANT and enabled=false
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(Role.APPLICANT);
        assertThat(savedUser.getEnabled()).isFalse();
    }

    @Property(tries = 100)
    void registeredUserApplicationHasPendingInitialReviewStatus(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String username,
            @ForAll @StringLength(min = 6, max = 100) String password) {

        // Arrange
        UserRepository userRepository = mock(UserRepository.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthServiceImpl authService = new AuthServiceImpl(userRepository, applicationRepository, passwordEncoder);
        RegisterRequest request = new RegisterRequest(username, password, Map.of("q1", "a1"));

        // Act
        authService.register(request);

        // Assert: saved application has status=PENDING_INITIAL_REVIEW
        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        Application savedApp = appCaptor.getValue();

        assertThat(savedApp.getStatus()).isEqualTo(ApplicationStatus.PENDING_INITIAL_REVIEW);
    }
}
