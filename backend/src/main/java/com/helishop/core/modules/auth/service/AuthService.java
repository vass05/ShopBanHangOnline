package com.helishop.core.modules.auth.service;

import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.auth.dto.AuthResponse;
import com.helishop.core.modules.auth.dto.ForgotPasswordRequest;
import com.helishop.core.modules.auth.dto.LoginRequest;
import com.helishop.core.modules.auth.dto.RefreshTokenRequest;
import com.helishop.core.modules.auth.dto.RegisterRequest;
import com.helishop.core.modules.auth.dto.ResetPasswordRequest;
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
    private final PasswordResetService passwordResetService;
    private final RegistrationOtpService registrationOtpService;

    @Transactional // giúp đảm bảo tính toàn vẽn dữ liệu
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ROLE_ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không được tự đăng ký quyền quản trị viên");
        }
        String finalEmail = RegistrationOtpService.gmail(request.getEmail());
        if (userRepository.existsByEmail(finalEmail)) {
            throw new AppException(ErrorCode.USER_EXISTED, "Gmail này đã được sử dụng");
        }
        registrationOtpService.consume(finalEmail, request.getOtp());
        String resolvedFullName = request.getFullName() == null || request.getFullName().isBlank()
                ? "User " + finalEmail.split("@")[0] : request.getFullName().trim();

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.ROLE_CUSTOMER;

        User user = User.builder()
                .email(finalEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(resolvedFullName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        return issueSession(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (!identifier.contains("@") || identifier.endsWith("@phone.helishop.com")) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng đăng nhập bằng email, không dùng số điện thoại");
        }
        User user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED,
                        "Gmail hoặc mật khẩu không đúng"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Tài khoản hiện đang bị khóa hoặc chưa kích hoạt");
        }

        if (user.getPasswordHash() == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Tài khoản này dùng Google. Vui lòng chọn Tiếp tục với Google");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Mật khẩu không chính xác");
        }

        return issueSession(user);
    }

    public AuthResponse issueSession(User user) {
        // Sinh cặp Access Token (15m) và Refresh Token (7 ngày)
        String accessToken = jwtUtils.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name(),
                user.getFullName(), user.getAuthVersion());
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
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Refresh Token không được để trống");
        }

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
                user.getFullName(), user.getAuthVersion());

        long expiresIn = jwtUtils.getAccessTokenExpirationMs() / 1000;

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.deleteRefreshToken(refreshToken);
    }

    public String sendForgotPasswordOtp(ForgotPasswordRequest request) {
        return passwordResetService.send(request);
    }

    public String resetPassword(ResetPasswordRequest request) {
        return passwordResetService.reset(request);
    }
}
