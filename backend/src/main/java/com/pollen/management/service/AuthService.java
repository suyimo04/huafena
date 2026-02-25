package com.pollen.management.service;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.LoginRequest;
import com.pollen.management.dto.LoginResponse;
import com.pollen.management.dto.RegisterRequest;

public interface AuthService {

    ApiResponse<?> register(RegisterRequest request);

    ApiResponse<LoginResponse> login(LoginRequest request);
}
