package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口 - 仅 ADMIN 可访问
 * 路径前缀 /api/admin/users，由 SecurityConfig 中 /api/admin/** hasRole("ADMIN") 保护
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 获取所有用户列表 */
    @GetMapping
    public ApiResponse<List<User>> listAll() {
        List<User> users = userRepository.findAll();
        // 不返回密码
        users.forEach(u -> u.setPassword(null));
        return ApiResponse.success(users);
    }

    /** 根据ID获取用户 */
    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    /** 创建用户 */
    @PostMapping
    public ApiResponse<User> create(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String roleStr = body.get("role");
        String enabledStr = body.get("enabled");

        if (username == null || password == null || roleStr == null) {
            throw new RuntimeException("用户名、密码和角色不能为空");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        Role role = Role.valueOf(roleStr);
        boolean enabled = "true".equalsIgnoreCase(enabledStr);

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(enabled)
                .build();
        userRepository.save(user);
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    /** 更新用户（角色、启用状态） */
    @PutMapping("/{id}")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (body.containsKey("role")) {
            user.setRole(Role.valueOf(body.get("role")));
        }
        if (body.containsKey("enabled")) {
            user.setEnabled("true".equalsIgnoreCase(body.get("enabled")));
        }
        if (body.containsKey("password") && !body.get("password").isEmpty()) {
            user.setPassword(passwordEncoder.encode(body.get("password")));
        }

        userRepository.save(user);
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(id);
        return ApiResponse.success(null);
    }
}
