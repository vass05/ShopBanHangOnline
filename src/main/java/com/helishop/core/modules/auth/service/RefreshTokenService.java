package com.helishop.core.modules.auth.service;

import com.helishop.core.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String RT_PREFIX = "auth:refresh_token:";
    private static final String USER_RT_PREFIX = "auth:user_rt:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;

    /**
     * Lưu Refresh Token vào Redis với thời gian hết hạn TTL 7 ngày
     */
    public void saveRefreshToken(String email, String refreshToken) {
        long ttlSeconds = jwtUtils.getRefreshTokenExpirationSec();
        String tokenKey = RT_PREFIX + refreshToken;
        String userKey = USER_RT_PREFIX + email;

        // Xóa token cũ của người dùng này nếu có (Single Session / Token Rotation)
        String oldToken = stringRedisTemplate.opsForValue().get(userKey);
        if (oldToken != null) {
            stringRedisTemplate.delete(RT_PREFIX + oldToken);
        }

        stringRedisTemplate.opsForValue().set(tokenKey, email, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(userKey, refreshToken, ttlSeconds, TimeUnit.SECONDS);

        log.info("Đã lưu Refresh Token vào Redis cho user '{}' với TTL {}s", email, ttlSeconds);
    }

    /**
     * Lấy email người dùng từ Refresh Token nếu hợp lệ trong Redis
     */
    public String getEmailFromRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(RT_PREFIX + refreshToken);
    }

    /**
     * Kiểm tra Refresh Token còn hiệu lực trong Redis không
     */
    public boolean isValidRefreshToken(String refreshToken) {
        String email = getEmailFromRefreshToken(refreshToken);
        return email != null && !email.isBlank();
    }

    /**
     * Xóa Refresh Token khi người dùng Đăng xuất (Logout)
     */
    public void deleteRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String tokenKey = RT_PREFIX + refreshToken;
        String email = stringRedisTemplate.opsForValue().get(tokenKey);

        stringRedisTemplate.delete(tokenKey);
        if (email != null) {
            stringRedisTemplate.delete(USER_RT_PREFIX + email);
        }
        log.info("Đã thu hồi Refresh Token khỏi Redis: {}", refreshToken);
    }

    /**
     * Thu hồi toàn bộ Refresh Token của một người dùng theo email
     */
    public void deleteByUserEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String userKey = USER_RT_PREFIX + email;
        String token = stringRedisTemplate.opsForValue().get(userKey);
        if (token != null) {
            stringRedisTemplate.delete(RT_PREFIX + token);
        }
        stringRedisTemplate.delete(userKey);
        log.info("Đã thu hồi Refresh Token cho user: {}", email);
    }
}
