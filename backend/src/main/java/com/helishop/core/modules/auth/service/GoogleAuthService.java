package com.helishop.core.modules.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.*;
import com.helishop.core.modules.auth.dto.*;
import com.helishop.core.modules.auth.service.GoogleTokenVerifier.GoogleIdentity;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;

@Service @RequiredArgsConstructor
public class GoogleAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> FAIL = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            local count = redis.call('INCR', KEYS[2])
            if count == 1 then redis.call('EXPIRE', KEYS[2], 300) end
            if count >= 5 then redis.call('DEL', KEYS[1]) end
            return count
            """, Long.class);
    private final GoogleTokenVerifier verifier;
    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final PasswordEncoder passwords;
    private final AuthService auth;

    public String challenge() {
        verifier.requireConfigured();
        String nonce = random();
        redis.opsForValue().set("auth:google:nonce:" + hash(nonce), "1", TTL);
        return nonce;
    }

    @Transactional(readOnly = true)
    public GoogleAuthResult login(String credential, String nonce) {
        GoogleIdentity identity = verifier.verify(credential, nonce);
        if (!"1".equals(redis.opsForValue().getAndDelete("auth:google:nonce:" + hash(nonce)))) throw expired();
        User linked = users.findByGoogleSubject(identity.subject()).orElse(null);
        if (linked != null) return authenticated(linked);
        User existing = users.findByEmail(identity.email()).orElse(null);
        if (existing != null) {
            requireActive(existing);
            if (existing.getGoogleSubject() != null || existing.getPasswordHash() == null) throw conflict();
        }
        String ticket = random();
        String value = encode(new Pending(identity, hash(nonce)));
        redis.opsForValue().set(pendingKey(ticket), value, TTL);
        return new GoogleAuthResult(existing == null ? "NEW_ACCOUNT" : "LINK_REQUIRED", null, ticket, identity.email(), identity.fullName());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GoogleAuthResult complete(GoogleCompleteRequest request, String nonce) {
        verifier.requireConfigured();
        if (nonce == null || !nonce.matches("[a-f0-9]{64}")) throw expired();
        if (request.role() == UserRole.ROLE_ADMIN) throw new AppException(ErrorCode.UNAUTHORIZED, "Không được tự đăng ký quyền quản trị viên");
        String key = pendingKey(request.ticket());
        String value = redis.opsForValue().get(key);
        if (value == null) throw expired();
        Pending pending = decode(value);
        if (!MessageDigest.isEqual(hash(nonce).getBytes(StandardCharsets.UTF_8), pending.binding().getBytes(StandardCharsets.UTF_8))) throw expired();
        GoogleIdentity identity = pending.identity();
        User user = users.findByGoogleSubject(identity.subject()).orElse(null);
        if (user == null) {
            user = users.findByEmailWithLock(identity.email()).orElse(null);
            if (user != null) {
                requireActive(user);
                if (user.getGoogleSubject() != null && !identity.subject().equals(user.getGoogleSubject())) throw conflict();
                if (user.getGoogleSubject() == null && (user.getPasswordHash() == null || request.password() == null
                        || !passwords.matches(request.password(), user.getPasswordHash()))) {
                    redis.execute(FAIL, List.of(key, key + ":attempts"), value);
                    throw new AppException(ErrorCode.UNAUTHENTICATED, "Cần mật khẩu hiện tại để liên kết tài khoản. Tối đa 5 lần thử, sau đó hãy đăng nhập Google lại");
                }
            } else {
                if (request.role() == null) throw new AppException(ErrorCode.INVALID_REQUEST, "Hãy chọn vai trò người mua hoặc người bán");
                user = User.builder().email(identity.email()).fullName(identity.fullName()).role(request.role()).status(UserStatus.ACTIVE).build();
            }
        }
        requireActive(user);
        if (!Long.valueOf(1).equals(redis.execute(CONSUME, List.of(key, key + ":attempts"), value))) throw expired();
        user.setGoogleSubject(identity.subject());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, "Tài khoản vừa được cập nhật ở nơi khác. Vui lòng đăng nhập Google lại");
        }
        return authenticated(user);
    }

    private GoogleAuthResult authenticated(User user) { requireActive(user); return GoogleAuthResult.authenticated(auth.issueSession(user)); }
    private void requireActive(User user) { if (user.getStatus() != UserStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHENTICATED, "Tài khoản đang bị khóa hoặc ngừng hoạt động"); }
    private AppException conflict() { return new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản đã liên kết một định danh Google khác. Vui lòng dùng cách đăng nhập đã liên kết"); }
    private AppException expired() { return new AppException(ErrorCode.UNAUTHENTICATED, "Phiên xác thực Google đã hết hạn hoặc đã dùng. Vui lòng thử lại"); }
    private String pendingKey(String ticket) { return "auth:google:pending:" + hash(ticket); }
    private String encode(Pending pending) {
        try { return mapper.writeValueAsString(pending); }
        catch (JsonProcessingException exception) { throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tạo được phiên đăng nhập Google"); }
    }
    private Pending decode(String value) {
        try { return mapper.readValue(value, Pending.class); }
        catch (JsonProcessingException exception) { throw expired(); }
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
    private static String random() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    record Pending(GoogleIdentity identity, String binding) {}
}
