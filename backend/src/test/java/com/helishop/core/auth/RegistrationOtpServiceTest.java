package com.helishop.core.auth;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.auth.service.RegistrationOtpService;
import com.helishop.core.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationOtpServiceTest {
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @Mock PasswordEncoder encoder;
    @Mock JavaMailSender mailSender;
    @Mock UserRepository users;
    @InjectMocks RegistrationOtpService service;
    private static final String EMAIL = "buyer@gmail.com";

    @Test void sendsOnlyHashToRedisAndCodeOnlyInMailWithoutCreatingAccount() {
        when(encoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(1L);
        String result = service.send("BUYER@gmail.com");
        var mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertEquals(EMAIL, mail.getValue().getTo()[0]);
        assertTrue(mail.getValue().getText().matches("(?s).*: [0-9]{6}.*"));
        String code = mail.getValue().getText().split(": ")[1].substring(0, 6);
        assertFalse(result.contains(code));
        verify(redis).execute(any(), anyList(), eq("bcrypt-hash"));
        verify(users, never()).save(any());
    }
    @Test void rejectsPhoneAndNonGmail() {
        assertThrows(AppException.class, () -> service.send("0901234567"));
        assertThrows(AppException.class, () -> service.send("buyer@example.com"));
        verifyNoInteractions(mailSender, redis);
    }
    @Test void duplicateDoesNotSendCode() {
        when(users.existsByEmail(EMAIL)).thenReturn(true);
        assertThrows(AppException.class, () -> service.send(EMAIL));
        verifyNoInteractions(mailSender, redis);
    }
    @Test void limitsSending() {
        when(encoder.encode(anyString())).thenReturn("hash");
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(0L);
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, assertThrows(AppException.class, () -> service.send(EMAIL)).getErrorCode());
        verifyNoInteractions(mailSender);
    }
    @Test void smtpFailureInvalidatesCodeAndReportsFailure() {
        when(encoder.encode(anyString())).thenReturn("hash");
        when(redis.<Long>execute(any(), anyList(), any())).thenReturn(1L);
        doThrow(new MailSendException("failure")).when(mailSender).send(any(SimpleMailMessage.class));
        assertEquals(ErrorCode.EMAIL_DELIVERY_FAILED, assertThrows(AppException.class, () -> service.send(EMAIL)).getErrorCode());
        verify(redis).execute(any(), anyList(), eq("hash"), eq("1"));
    }
    private void cached() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:registration:" + EMAIL)).thenReturn("hash");
    }
    @Test void wrongCodeCountsAttempt() {
        cached();
        assertThrows(AppException.class, () -> service.consume(EMAIL, "123456"));
        verify(redis).execute(any(), anyList(), eq("hash"), eq("0"));
    }
    @Test void correctCodeMustWinAtomicConsumption() {
        cached();
        when(encoder.matches("123456", "hash")).thenReturn(true);
        when(redis.<Long>execute(any(), anyList(), eq("hash"), eq("1"))).thenReturn(0L);
        assertThrows(AppException.class, () -> service.consume(EMAIL, "123456"));
    }
    @Test void correctCodeConsumedSuccessfully() {
        cached();
        when(encoder.matches("123456", "hash")).thenReturn(true);
        when(redis.<Long>execute(any(), anyList(), eq("hash"), eq("1"))).thenReturn(1L);
        assertDoesNotThrow(() -> service.consume(EMAIL, "123456"));
    }
    @Test void expiredCodeRejectedWithoutHashCheck() {
        when(redis.opsForValue()).thenReturn(values);
        assertThrows(AppException.class, () -> service.consume(EMAIL, "123456"));
        verifyNoInteractions(encoder);
    }
}
