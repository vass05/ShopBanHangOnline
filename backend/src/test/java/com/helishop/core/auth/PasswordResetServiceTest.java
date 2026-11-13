package com.helishop.core.auth;

import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.auth.dto.ForgotPasswordRequest;
import com.helishop.core.modules.auth.dto.ResetPasswordRequest;
import com.helishop.core.modules.auth.service.PasswordResetService;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock UserRepository users;
    @Mock StringRedisTemplate redis;
    @Mock JavaMailSender mailSender;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenService refreshTokens;
    @Mock ValueOperations<String, String> values;
    @InjectMocks PasswordResetService service;
    User user;

    @BeforeEach
    void setup() {
        user = User.builder().email("buyer@test.com").status(UserStatus.ACTIVE).passwordHash("old").build();
        user.setId(1L);
    }

    private ForgotPasswordRequest forgot() { return ForgotPasswordRequest.builder().email(user.getEmail()).build(); }
    private ResetPasswordRequest reset() { return ResetPasswordRequest.builder().email(user.getEmail()).otp("123456").newPassword("NewPassword123!").build(); }

    @Test
    void sendsOtpOnlyInEmailAndStoresOnlyHash() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(1L);
        String result = service.send(forgot());
        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertTrue(email.getValue().getText().matches("(?s).*OTP.*[0-9]{6}.*"));
        assertFalse(result.matches("(?s).*[0-9]{6}.*"));
        verify(redis).execute(any(), anyList(), eq("bcrypt-hash"));
    }

    @Test
    void rejectsResendWhenRedisLimitIsReached() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.encode(anyString())).thenReturn("hash");
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(0L);
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, assertThrows(AppException.class, () -> service.send(forgot())).getErrorCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void smtpFailureRevokesIssuedOtpAndReturnsServiceError() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.encode(anyString())).thenReturn("hash");
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(1L);
        doThrow(new MailSendException("SMTP failed")).when(mailSender).send(any(SimpleMailMessage.class));
        assertEquals(ErrorCode.EMAIL_DELIVERY_FAILED, assertThrows(AppException.class, () -> service.send(forgot())).getErrorCode());
        verify(redis, times(2)).execute(any(), anyList(), eq("hash"));
    }

    @Test
    void missingAccountReturnsGenericMessageWithoutSendingEmail() {
        assertFalse(service.send(forgot()).matches("(?s).*[0-9]{6}.*"));
        verifyNoInteractions(mailSender, redis);
    }

    private void cachedOtp() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:password_reset:" + user.getEmail())).thenReturn("hash");
    }

    @Test
    void wrongOtpCountsAttemptAndDoesNotChangePassword() {
        cachedOtp();
        assertThrows(AppException.class, () -> service.reset(reset()));
        verify(redis).execute(any(), anyList(), eq("hash"));
        verify(users, never()).save(any());
    }

    @Test
    void validOtpMustBeConsumedAtomicallyBeforePasswordUpdate() {
        cachedOtp();
        when(encoder.matches("123456", "hash")).thenReturn(true);
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(1L);
        when(encoder.encode("NewPassword123!")).thenReturn("new-hash");
        service.reset(reset());
        assertEquals("new-hash", user.getPasswordHash());
        verify(users).save(user);
        verify(refreshTokens).deleteByUserEmail(user.getEmail());
    }

    @Test
    void reusedOtpCannotUpdatePasswordAgain() {
        cachedOtp();
        when(encoder.matches("123456", "hash")).thenReturn(true);
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(0L);
        assertThrows(AppException.class, () -> service.reset(reset()));
        verify(users, never()).save(any());
        verifyNoInteractions(refreshTokens);
    }
}
