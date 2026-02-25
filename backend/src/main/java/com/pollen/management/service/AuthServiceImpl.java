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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public ApiResponse<?> register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(409, "用户名已存在");
        }

        // Create user with role=APPLICANT, enabled=false, BCrypt-encoded password
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.APPLICANT)
                .enabled(false)
                .build();
        userRepository.save(user);

        // Create application with status=PENDING_INITIAL_REVIEW, entryType=REGISTRATION
        Application application = Application.builder()
                .userId(user.getId())
                .status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION)
                .build();
        applicationRepository.save(application);

        return ApiResponse.success("注册成功，请等待审核");
    }

    @Override
    public ApiResponse<LoginResponse> login(LoginRequest request) {
        // Find user by username - generic error if not found
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        // Check password - same generic error if wrong
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // Check enabled status - specific message for disabled accounts
        if (!user.getEnabled()) {
            throw new BusinessException(403, "账户待审核，暂时无法登录");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return ApiResponse.success(new LoginResponse(token));
    }
}
