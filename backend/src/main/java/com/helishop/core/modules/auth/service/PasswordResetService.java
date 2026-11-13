package com.helishop.core.modules.auth.service;

import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.auth.dto.ForgotPasswordRequest;
import com.helishop.core.modules.auth.dto.ResetPasswordRequest;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SENT = "Nếu tài khoản hợp lệ, mã OTP đã được gửi tới email đăng ký và có hiệu lực 10 phút.";
    private static final DefaultRedisScript<Long> ISSUE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            local count = tonumber(redis.call('GET', KEYS[3]) or '0')
            if count >= 5 then return 0 end
            count = redis.call('INCR', KEYS[3])
            if count == 1 then redis.call('EXPIRE', KEYS[3], 3600) end
            redis.call('SET', KEYS[2], '1', 'EX', 60)
            redis.call('SET', KEYS[1], ARGV[1], 'EX', 600)
            redis.call('DEL', KEYS[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> FAIL = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then redis.call('EXPIRE', KEYS[2], 600) end
            if attempts >= 5 then redis.call('DEL', KEYS[1]) end
            return attempts
            """, Long.class);

    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshTokens;

    @Value("${spring.mail.username:}")
    private String senderEmail = "";

    public String send(ForgotPasswordRequest request) {
        User user = lookup(request.getEmail());
        if (user == null || user.getStatus() != UserStatus.ACTIVE) return SENT;
        String key = "auth:password_reset:" + user.getEmail();
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        String hash = encoder.encode(otp);
        Long issued = redis.execute(ISSUE, List.of(key, key + ":cooldown", key + ":hour", key + ":attempts"), hash);
        if (!Long.valueOf(1).equals(issued)) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS, "Vui lòng chờ trước khi gửi lại OTP; tối đa 5 lần mỗi giờ");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (!senderEmail.isBlank()) message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("[HeliShop] Mã khôi phục mật khẩu");
        message.setText("Mã OTP khôi phục mật khẩu của bạn: " + otp + "\nMã có hiệu lực 10 phút. Không chia sẻ mã này với bất kỳ ai.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            redis.execute(CONSUME, List.of(key, key + ":attempts"), hash);
            log.warn("Không thể gửi email khôi phục mật khẩu cho userId={}", user.getId());
            throw new AppException(ErrorCode.EMAIL_DELIVERY_FAILED, "Chưa gửi được email khôi phục. Vui lòng thử lại sau");
        }
        return SENT;
    }

    @Transactional
    public String reset(ResetPasswordRequest request) {
        User user = lookup(request.getEmail());
        if (user == null || user.getStatus() != UserStatus.ACTIVE) throw invalidOtp();
        String key = "auth:password_reset:" + user.getEmail();
        String hash = redis.opsForValue().get(key);
        if (hash == null) throw invalidOtp();
        if (!encoder.matches(request.getOtp().trim(), hash)) {
            redis.execute(FAIL, List.of(key, key + ":attempts"), hash);
            throw invalidOtp();
        }
        Long consumed = redis.execute(CONSUME, List.of(key, key + ":attempts"), hash);
        if (!Long.valueOf(1).equals(consumed)) throw invalidOtp();
        user.setPasswordHash(encoder.encode(request.getNewPassword()));
        users.save(user);
        refreshTokens.deleteByUserEmail(user.getEmail());
        return "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.";
    }

    private User lookup(String identifier) {
        String normalized = identifier.trim();
        return users.findByEmail(normalized).or(() -> users.findByPhone(normalized)).orElse(null);
    }

    private AppException invalidOtp() {
        return new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không hợp lệ, đã dùng hoặc hết hạn. Vui lòng lấy mã mới");
    }
}
