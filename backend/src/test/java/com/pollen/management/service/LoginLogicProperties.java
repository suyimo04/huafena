package com.pollen.management.service;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.LoginRequest;
import com.pollen.management.dto.LoginResponse;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.security.JwtUtil;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 2: 登录需要正确凭据且账户启用
 * **Validates: Requirements 1.3, 1.4, 1.5**
 *
 * For any 用户名和密码组合以及账户状态，登录成功当且仅当凭据正确且账户状态为启用。
 * 凭据错误时返回通用错误信息（不区分用户名错误还是密码错误），账户禁用时返回待审核提示。
 */
class LoginLogicProperties {

    private AuthServiceImpl createService(UserRepository userRepository) {
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtUtil jwtUtil = new JwtUtil(
                "test-secret-key-that-is-at-least-32-bytes-long-for-hmac",
                3600000L
        );

        // PasswordEncoder: matches returns true only when raw equals "correctPassword"
        when(passwordEncoder.matches(eq("correctPassword"), eq("$2a$10$encodedHash"))).thenReturn(true);
        when(passwordEncoder.matches(argThat(arg -> !"correctPassword".equals(arg)), eq("$2a$10$encodedHash"))).thenReturn(false);

        return new AuthServiceImpl(userRepository, applicationRepository, passwordEncoder, jwtUtil);
    }

    private User buildUser(String username, Role role, boolean enabled) {
        return User.builder()
                .id(1L)
                .username(username)
                .password("$2a$10$encodedHash")
                .role(role)
                .enabled(enabled)
                .build();
    }

    /**
     * Scenario 1: For any correct credentials + enabled account → login succeeds (code 200, token not null)
     */
    @Property(tries = 100)
    void correctCredentialsAndEnabledAccountReturnsToken(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String username,
            @ForAll("roles") Role role) {

        UserRepository userRepository = mock(UserRepository.class);
        User user = buildUser(username, role, true);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        AuthServiceImpl authService = createService(userRepository);
        LoginRequest request = new LoginRequest(username, "correctPassword");

        ApiResponse<LoginResponse> response = authService.login(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getToken()).isNotNull().isNotEmpty();
    }

    /**
     * Scenario 2: For any wrong password + existing user → throws 401 with generic message
     */
    @Property(tries = 100)
    void wrongPasswordThrows401WithGenericMessage(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String username,
            @ForAll @StringLength(min = 1, max = 50) String wrongPassword,
            @ForAll("roles") Role role) {

        Assume.that(!"correctPassword".equals(wrongPassword));

        UserRepository userRepository = mock(UserRepository.class);
        User user = buildUser(username, role, true);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        AuthServiceImpl authService = createService(userRepository);
        LoginRequest request = new LoginRequest(username, wrongPassword);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(401);
                    assertThat(bex.getMessage()).isEqualTo("用户名或密码错误");
                });
    }

    /**
     * Scenario 3: For any nonexistent username → throws 401 with same generic message
     */
    @Property(tries = 100)
    void nonexistentUsernameThrows401WithGenericMessage(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String username,
            @ForAll @StringLength(min = 1, max = 50) String anyPassword) {

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        AuthServiceImpl authService = createService(userRepository);
        LoginRequest request = new LoginRequest(username, anyPassword);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(401);
                    assertThat(bex.getMessage()).isEqualTo("用户名或密码错误");
                });
    }

    /**
     * Scenario 4: For any correct credentials + disabled account → throws 403 with "账户待审核" message
     */
    @Property(tries = 100)
    void correctCredentialsButDisabledAccountThrows403(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String username,
            @ForAll("roles") Role role) {

        UserRepository userRepository = mock(UserRepository.class);
        User user = buildUser(username, role, false);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        AuthServiceImpl authService = createService(userRepository);
        LoginRequest request = new LoginRequest(username, "correctPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(403);
                    assertThat(bex.getMessage()).isEqualTo("账户待审核，暂时无法登录");
                });
    }

    /**
     * Scenario 5: Error messages for wrong username and wrong password are identical (no info leakage)
     */
    @Property(tries = 100)
    void errorMessagesForWrongUsernameAndWrongPasswordAreIdentical(
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String existingUsername,
            @ForAll @AlphaChars @StringLength(min = 2, max = 50) String nonexistentUsername,
            @ForAll @StringLength(min = 1, max = 50) String wrongPassword,
            @ForAll("roles") Role role) {

        Assume.that(!"correctPassword".equals(wrongPassword));
        Assume.that(!existingUsername.equals(nonexistentUsername));

        // Setup: existing user with wrong password
        UserRepository userRepo1 = mock(UserRepository.class);
        User user = buildUser(existingUsername, role, true);
        when(userRepo1.findByUsername(existingUsername)).thenReturn(Optional.of(user));
        AuthServiceImpl service1 = createService(userRepo1);

        // Setup: nonexistent user
        UserRepository userRepo2 = mock(UserRepository.class);
        when(userRepo2.findByUsername(nonexistentUsername)).thenReturn(Optional.empty());
        AuthServiceImpl service2 = createService(userRepo2);

        // Capture both exceptions
        BusinessException wrongPasswordEx = null;
        BusinessException wrongUsernameEx = null;

        try {
            service1.login(new LoginRequest(existingUsername, wrongPassword));
        } catch (BusinessException e) {
            wrongPasswordEx = e;
        }

        try {
            service2.login(new LoginRequest(nonexistentUsername, wrongPassword));
        } catch (BusinessException e) {
            wrongUsernameEx = e;
        }

        assertThat(wrongPasswordEx).isNotNull();
        assertThat(wrongUsernameEx).isNotNull();
        assertThat(wrongPasswordEx.getCode()).isEqualTo(wrongUsernameEx.getCode());
        assertThat(wrongPasswordEx.getMessage()).isEqualTo(wrongUsernameEx.getMessage());
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }
}
