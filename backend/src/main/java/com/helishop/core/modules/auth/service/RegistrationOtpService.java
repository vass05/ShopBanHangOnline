package com.helishop.core.modules.auth.service;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationOtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
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
    // The hash comparison prevents a stale request from consuming/counting against a newer code.
    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            if ARGV[2] == '1' then
              redis.call('DEL', KEYS[1], KEYS[2])
              return 1
            end
            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then redis.call('EXPIRE', KEYS[2], 600) end
            if attempts >= 5 then redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);
    private final StringRedisTemplate redis;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final UserRepository users;
    @Value("${spring.mail.username:}")
    private String senderEmail = "";

    public static String gmail(String input) {
        String email = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 150 || !email.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@gmail\\.com")) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng nhập địa chỉ @gmail.com");
        }
        return email;
    }

    public String send(String input) {
        String email = gmail(input);
        if (users.existsByEmail(email)) throw new AppException(ErrorCode.USER_EXISTED, "Gmail này đã có tài khoản. Vui lòng đăng nhập");
        String key = key(email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String hash = encoder.encode(code);
        if (!Long.valueOf(1).equals(redis.execute(ISSUE, List.of(key, key + ":cooldown", key + ":hour", key + ":attempts"), hash))) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS, "Chờ 60 giây trước khi gửi lại OTP; tối đa 5 lần mỗi giờ");
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        if (!senderEmail.isBlank()) mail.setFrom(senderEmail);
        mail.setTo(email);
        mail.setSubject("[HeliShop] Xác thực đăng ký tài khoản");
        mail.setText("Mã OTP đăng ký HeliShop: " + code + "\nMã có hiệu lực 10 phút và chỉ dùng một lần. Không chia sẻ mã này.");
        try {
            mailSender.send(mail);
        } catch (MailException exception) {
            redis.execute(VERIFY, List.of(key, key + ":attempts"), hash, "1");
            throw new AppException(ErrorCode.EMAIL_DELIVERY_FAILED, "Chưa gửi được OTP. Vui lòng kiểm tra cấu hình gửi email hoặc thử lại sau");
        }
        return "OTP đã được gửi về Gmail, có hiệu lực 10 phút. Kiểm tra cả thư mục Spam.";
    }

    public void consume(String email, String code) {
        String key = key(email);
        String hash = redis.opsForValue().get(key);
        if (hash == null) throw invalid();
        boolean matches = code != null && code.matches("[0-9]{6}") && encoder.matches(code, hash);
        Long result = redis.execute(VERIFY, List.of(key, key + ":attempts"), hash, matches ? "1" : "0");
        if (!matches || !Long.valueOf(1).equals(result)) throw invalid();
    }

    private static String key(String email) { return "auth:registration:" + email; }
    private static AppException invalid() {
        return new AppException(ErrorCode.INVALID_REQUEST, "OTP không đúng, đã dùng hoặc hết hạn. Vui lòng lấy mã mới");
    }
}
