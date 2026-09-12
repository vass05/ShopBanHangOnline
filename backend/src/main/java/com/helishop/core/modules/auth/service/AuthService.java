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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean hasEmail = request.getEmail() != null && !request.getEmail().trim().isBlank();
        boolean hasPhone = request.getPhone() != null && !request.getPhone().trim().isBlank();

        if (!hasEmail && !hasPhone) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng cung cấp ít nhất một phương thức liên lạc: Địa chỉ Gmail hoặc Số điện thoại");
        }

        String finalEmail = hasEmail ? request.getEmail().trim() : null;
        String finalPhone = hasPhone ? request.getPhone().trim() : null;

        if (hasEmail && userRepository.existsByEmail(finalEmail)) {
            throw new AppException(ErrorCode.USER_EXISTED, "Địa chỉ Gmail này đã được sử dụng. Vui lòng chọn Gmail khác hoặc đăng nhập.");
        }

        if (hasPhone && userRepository.existsByPhone(finalPhone)) {
            throw new AppException(ErrorCode.USER_EXISTED, "Số điện thoại này đã được liên kết với một tài khoản khác.");
        }

        // Nếu đăng ký bằng Số điện thoại mà không nhập Gmail, tạo định danh email hệ thống
        if (finalEmail == null) {
            finalEmail = finalPhone + "@phone.helishop.com";
        }

        // Họ tên mặc định nếu chưa cập nhật
        String resolvedFullName;
        if (request.getFullName() != null && !request.getFullName().trim().isBlank()) {
            resolvedFullName = request.getFullName().trim();
        } else if (hasEmail) {
            resolvedFullName = "User " + finalEmail.split("@")[0];
        } else if (hasPhone) {
            resolvedFullName = "User " + finalPhone;
        } else {
            resolvedFullName = "Thành viên HeliShop";
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.ROLE_CUSTOMER;

        User user = User.builder()
                .email(finalEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(resolvedFullName)
                .phone(finalPhone)
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
                .phone(savedUser.getPhone())
                .role(savedUser.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmail().trim();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Tài khoản (Email hoặc Số điện thoại) không tồn tại trên hệ thống"));

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
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.deleteRefreshToken(refreshToken);
    }

    /**
     * Gửi mã OTP xác thực quên mật khẩu qua Redis và Email
     */
    public String sendForgotPasswordOtp(ForgotPasswordRequest request) {
        String identifier = request.getEmail().trim();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy tài khoản với Gmail/SĐT: " + identifier));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Tài khoản hiện đang bị khóa hoặc ngưng hoạt động");
        }

        // Tạo mã OTP 6 chữ số ngẫu nhiên
        String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);
        String redisKey = "auth:password_reset:" + user.getEmail();

        // Lưu vào Redis với TTL 10 phút
        stringRedisTemplate.opsForValue().set(redisKey, otp, 10, TimeUnit.MINUTES);
        log.info("Đã tạo mã OTP khôi phục mật khẩu cho {}: {}", user.getEmail(), otp);

        // Gửi email nếu JavaMailSender sẵn sàng
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(user.getEmail());
                mailMessage.setSubject("[HeliShop] Mã xác thực lấy lại mật khẩu");
                mailMessage.setText("Xin chào " + user.getFullName() + ",\n\n"
                        + "Mã OTP khôi phục mật khẩu tài khoản HeliShop của bạn là: " + otp + "\n"
                        + "Mã này có hiệu lực trong vòng 10 phút. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.\n\n"
                        + "Trân trọng,\nĐội ngũ HeliShop Core.");
                mailSender.send(mailMessage);
                log.info("Đã gửi email OTP thành công tới {}", user.getEmail());
            } catch (Exception e) {
                log.warn("Không thể gửi email OTP qua SMTP tới {}: {}", user.getEmail(), e.getMessage());
            }
        }

        return "Mã xác thực OTP đã được gửi thành công (Mã thử nghiệm: " + otp + "). Mã có hiệu lực trong 10 phút.";
    }

    /**
     * Đặt lại mật khẩu mới sau khi xác thực OTP thành công
     */
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        String identifier = request.getEmail().trim();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy tài khoản để đặt lại mật khẩu"));

        String redisKey = "auth:password_reset:" + user.getEmail();
        String cachedOtp = stringRedisTemplate.opsForValue().get(redisKey);

        if (cachedOtp == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã xác thực OTP đã hết hạn hoặc chưa được tạo. Vui lòng lấy mã mới!");
        }

        if (!cachedOtp.equals(request.getOtp().trim())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại!");
        }

        // Cập nhật mật khẩu mới băm BCrypt
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);

        // Xóa mã OTP khỏi Redis
        stringRedisTemplate.delete(redisKey);
        log.info("Đặt lại mật khẩu thành công cho tài khoản {}", user.getEmail());

        return "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới ngay bây giờ.";
    }
}
