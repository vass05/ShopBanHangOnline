package com.helishop.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    // Secret key 256 bits (64 hex characters)
    private static final String SECRET_256_BITS = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET_256_BITS);
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationMs", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpirationSec", 604800L); // 7 days
    }

    @Test
    @DisplayName("Sinh Access Token hợp lệ và trích xuất đúng các claims (email, userId, role, fullName)")
    void shouldGenerateValidAccessTokenAndExtractClaims() {
        String token = jwtUtils.generateAccessToken("customer@helishop.com", 101L, "ROLE_CUSTOMER", "Nguyễn Văn Test");

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("customer@helishop.com", jwtUtils.getUsernameFromJwtToken(token));
        assertEquals(101L, jwtUtils.getUserIdFromJwtToken(token));
        assertEquals("ROLE_CUSTOMER", jwtUtils.getRoleFromJwtToken(token));
        assertEquals("Nguyễn Văn Test", jwtUtils.getFullNameFromJwtToken(token));
    }

    @Test
    @DisplayName("Sinh Refresh Token ngẫu nhiên không rỗng và có độ dài thích hợp")
    void shouldGenerateValidRefreshTokenString() {
        String refreshToken1 = jwtUtils.generateRefreshToken();
        String refreshToken2 = jwtUtils.generateRefreshToken();

        assertNotNull(refreshToken1);
        assertNotNull(refreshToken2);
        assertFalse(refreshToken1.isBlank());
        assertFalse(refreshToken1.equals(refreshToken2));
    }

    @Test
    @DisplayName("Từ chối token bị sửa đổi hoặc giả mạo chữ ký HMAC-SHA256")
    void shouldRejectTamperedToken() {
        String validToken = jwtUtils.generateAccessToken("seller@helishop.com", 202L, "ROLE_SELLER", "Trần Shop");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        assertFalse(jwtUtils.validateJwtToken(tamperedToken));
    }

    @Test
    @DisplayName("Từ chối token đã quá hạn sử dụng")
    void shouldRejectExpiredToken() {
        // Thiết lập thời gian sống âm để token hết hạn ngay tức thì
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationMs", -1000L);

        String expiredToken = jwtUtils.generateAccessToken("expired@helishop.com", 303L, "ROLE_ADMIN", "Admin User");
        assertFalse(jwtUtils.validateJwtToken(expiredToken));
    }
}
