package com.helishop.core.auth;

import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.modules.auth.dto.AuthResponse;
import com.helishop.core.modules.auth.dto.LoginRequest;
import com.helishop.core.modules.auth.dto.RefreshTokenRequest;
import com.helishop.core.modules.auth.dto.RegisterRequest;
import com.helishop.core.modules.auth.service.AuthService;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock private com.helishop.core.modules.auth.service.RegistrationOtpService registrationOtpService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .email("test@gmail.com")
                .passwordHash("hashed_password_xyz")
                .fullName("Test User")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("Đăng ký thành công: mã hóa mật khẩu, tạo cặp token và lưu Refresh Token vào Redis")
    void shouldRegisterUserSuccessfullyAndSaveRefreshToken() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@gmail.com")
                .password("plain_password").otp("123456")
                .fullName("Test User")
                .role(UserRole.ROLE_CUSTOMER)
                .build();

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("plain_password")).thenReturn("hashed_password_xyz");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtils.generateAccessToken(anyString(), anyLong(), anyString(), anyString())).thenReturn("mock_access_token");
        when(jwtUtils.generateRefreshToken()).thenReturn("mock_refresh_token");
        when(jwtUtils.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.register(request);
        verify(registrationOtpService).consume("test@gmail.com", "123456");

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        assertEquals(900L, response.getExpiresIn());
        verify(refreshTokenService).saveRefreshToken(eq("test@gmail.com"), eq("mock_refresh_token"));
    }

    @Test
    @DisplayName("Đăng nhập thành công với mật khẩu đúng: sinh cặp token và lưu Refresh Token")
    void shouldLoginSuccessfullyWhenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder()
                .email("test@gmail.com")
                .password("correct_pass")
                .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correct_pass", "hashed_password_xyz")).thenReturn(true);
        when(jwtUtils.generateAccessToken(anyString(), anyLong(), anyString(), anyString())).thenReturn("mock_access_token");
        when(jwtUtils.generateRefreshToken()).thenReturn("mock_refresh_token");
        when(jwtUtils.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        verify(refreshTokenService).saveRefreshToken(eq("test@gmail.com"), eq("mock_refresh_token"));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu: ném AppException")
    void shouldThrowExceptionWhenLoginWithWrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("test@gmail.com")
                .password("wrong_pass")
                .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "hashed_password_xyz")).thenReturn(false);

        assertThrows(AppException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Cấp mới Access Token thành công khi Refresh Token hợp lệ trong Redis")
    void shouldRefreshTokenSuccessfullyWhenTokenValidInRedis() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid_refresh_token_123")
                .build();

        when(refreshTokenService.getEmailFromRefreshToken("valid_refresh_token_123")).thenReturn("test@gmail.com");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(jwtUtils.generateAccessToken(anyString(), anyLong(), anyString(), anyString())).thenReturn("new_access_token");
        when(jwtUtils.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("valid_refresh_token_123", response.getRefreshToken());
    }

    @Test
    @DisplayName("Cấp mới Access Token thất bại khi Refresh Token không tồn tại trong Redis")
    void shouldThrowExceptionWhenRefreshTokenNotFoundInRedis() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid_or_expired_token")
                .build();

        when(refreshTokenService.getEmailFromRefreshToken("invalid_or_expired_token")).thenReturn(null);

        assertThrows(AppException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("Đăng xuất: gọi thu hồi Refresh Token trong Redis")
    void shouldCallDeleteRefreshTokenOnLogout() {
        authService.logout("refresh_token_to_delete");
        verify(refreshTokenService).deleteRefreshToken("refresh_token_to_delete");
    }
    @Test
    void cannotCreateUserOrTokensIfOtpIsRejected() {
        RegisterRequest request = RegisterRequest.builder().email("test@gmail.com").password("Password123!").otp("123456").build();
        org.mockito.Mockito.doThrow(new AppException(com.helishop.core.common.exception.ErrorCode.INVALID_REQUEST))
                .when(registrationOtpService).consume("test@gmail.com", "123456");
        assertThrows(AppException.class, () -> authService.register(request));
        verify(userRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(jwtUtils, refreshTokenService);
    }

    @Test
    void phoneIsRejectedWithoutLookingUpAccount() {
        assertThrows(AppException.class, () -> authService.login(LoginRequest.builder().email("0901234567").password("Password123!").build()));
        org.mockito.Mockito.verifyNoInteractions(userRepository, jwtUtils, refreshTokenService);
    }

}
