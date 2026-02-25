package com.pollen.management.service;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.LoginRequest;
import com.pollen.management.dto.LoginResponse;
import com.pollen.management.dto.RegisterRequest;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.security.JwtUtil;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest("testuser", "password123", Map.of("q1", "answer1"));
    }

    // --- Helper to build a User for login tests ---
    private User buildUser(Long id, String username, String encodedPassword, Role role, boolean enabled) {
        return User.builder()
                .id(id)
                .username(username)
                .password(encodedPassword)
                .role(role)
                .enabled(enabled)
                .build();
    }

    @Test
    void register_shouldCreateUserWithApplicantRoleAndDisabled() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<?> response = authService.register(validRequest);

        assertEquals(200, response.getCode());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(Role.APPLICANT, savedUser.getRole());
        assertFalse(savedUser.getEnabled());
        assertEquals("$2a$10$encodedPassword", savedUser.getPassword());
        assertEquals("testuser", savedUser.getUsername());
    }

    @Test
    void register_shouldCreateApplicationWithPendingInitialReview() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(validRequest);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        Application savedApp = appCaptor.getValue();
        assertEquals(ApplicationStatus.PENDING_INITIAL_REVIEW, savedApp.getStatus());
        assertEquals(EntryType.REGISTRATION, savedApp.getEntryType());
        assertEquals(1L, savedApp.getUserId());
    }

    @Test
    void register_shouldThrowWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(validRequest));
        assertEquals(409, ex.getCode());
        assertEquals("用户名已存在", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void register_shouldEncodePasswordWithBCrypt() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$bcryptHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(validRequest);

        verify(passwordEncoder).encode("password123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotEquals("password123", userCaptor.getValue().getPassword());
    }

    // --- Login tests ---

    @Test
    void login_shouldReturnTokenWhenCredentialsCorrectAndEnabled() {
        User user = buildUser(1L, "admin", "$2a$10$encoded", Role.ADMIN, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "$2a$10$encoded")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "admin", "ADMIN")).thenReturn("jwt-token-123");

        ApiResponse<LoginResponse> response = authService.login(new LoginRequest("admin", "admin123"));

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("jwt-token-123", response.getData().getToken());
    }

    @Test
    void login_shouldThrow401WhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("nonexistent", "password")));

        assertEquals(401, ex.getCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_shouldThrow401WhenPasswordWrong() {
        User user = buildUser(1L, "admin", "$2a$10$encoded", Role.ADMIN, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$2a$10$encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("admin", "wrongpass")));

        assertEquals(401, ex.getCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_shouldReturnSameErrorForWrongUsernameAndWrongPassword() {
        // Wrong username
        when(userRepository.findByUsername("wrong")).thenReturn(Optional.empty());
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("wrong", "password")));

        // Wrong password
        User user = buildUser(1L, "admin", "$2a$10$encoded", Role.ADMIN, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$2a$10$encoded")).thenReturn(false);
        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("admin", "wrongpass")));

        // Both should return the same generic error (no info leakage)
        assertEquals(ex1.getCode(), ex2.getCode());
        assertEquals(ex1.getMessage(), ex2.getMessage());
    }

    @Test
    void login_shouldThrow403WhenAccountDisabled() {
        User user = buildUser(1L, "applicant", "$2a$10$encoded", Role.APPLICANT, false);
        when(userRepository.findByUsername("applicant")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$encoded")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("applicant", "password123")));

        assertEquals(403, ex.getCode());
        assertEquals("账户待审核，暂时无法登录", ex.getMessage());
    }

    @Test
    void login_shouldNotGenerateTokenWhenAccountDisabled() {
        User user = buildUser(1L, "applicant", "$2a$10$encoded", Role.APPLICANT, false);
        when(userRepository.findByUsername("applicant")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$encoded")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("applicant", "password123")));

        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }
}
