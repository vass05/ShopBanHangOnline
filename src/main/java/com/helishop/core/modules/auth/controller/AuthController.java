package com.helishop.core.modules.auth.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.auth.dto.AuthResponse;
import com.helishop.core.modules.auth.dto.LoginRequest;
import com.helishop.core.modules.auth.dto.RefreshTokenRequest;
import com.helishop.core.modules.auth.dto.RegisterRequest;
import com.helishop.core.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs Đăng ký, Đăng nhập, Làm mới Token và Đăng xuất")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký tài khoản người dùng mới (Mặc định ROLE_CUSTOMER hoặc ROLE_SELLER)")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Đăng ký tài khoản thành công");
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập và nhận cặp Access Token (15p) + Refresh Token (7 ngày)")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Đăng nhập thành công");
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token khi hết hạn bằng Refresh Token từ Redis")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request), "Cấp mới access token thành công");
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất và thu hồi Refresh Token trong Redis")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ApiResponse.success(null, "Đăng xuất thành công");
    }
}
