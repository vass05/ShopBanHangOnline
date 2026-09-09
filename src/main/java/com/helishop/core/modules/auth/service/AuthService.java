package com.helishop.core.modules.auth.service;

import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.auth.dto.AuthResponse;
import com.helishop.core.modules.auth.dto.LoginRequest;
import com.helishop.core.modules.auth.dto.RefreshTokenRequest;
import com.helishop.core.modules.auth.dto.RegisterRequest;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import com.helishop.core.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.ROLE_CUSTOMER;

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // Sinh cặp Access Token (15m) và Refresh Token (7 ngày)
        String accessToken = jwtUtils.generateAccessToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getRole().name(),
                savedUser.getFullName()
        );
        String refreshToken = jwtUtils.generateRefreshToken();

        // Lưu Refresh Token vào Redis với TTL 7 ngày
        refreshTokenService.saveRefreshToken(savedUser.getEmail(), refreshToken);

        long expiresIn = jwtUtils.getAccessTokenExpirationMs() / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Tài khoản hiện đang bị khóa hoặc chưa kích hoạt");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Mật khẩu không chính xác");
        }

        // Sinh cặp Access Token (15m) và Refresh Token (7 ngày)
        String accessToken = jwtUtils.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name(),
                user.getFullName()
        );
        String refreshToken = jwtUtils.generateRefreshToken();

        // Lưu Refresh Token vào Redis
        refreshTokenService.saveRefreshToken(user.getEmail(), refreshToken);

        long expiresIn = jwtUtils.getAccessTokenExpirationMs() / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String email = refreshTokenService.getEmailFromRefreshToken(refreshToken);

        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Tài khoản người dùng không hoạt động");
        }

        // Cấp mới Access Token (15 phút)
        String newAccessToken = jwtUtils.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name(),
                user.getFullName()
        );

        long expiresIn = jwtUtils.getAccessTokenExpirationMs() / 1000;

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.deleteRefreshToken(refreshToken);
    }
}
