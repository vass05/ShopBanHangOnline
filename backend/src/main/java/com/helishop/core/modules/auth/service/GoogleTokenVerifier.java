package com.helishop.core.modules.auth.service;

import com.helishop.core.common.exception.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;

@Component
public class GoogleTokenVerifier {
    private final String clientId;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public GoogleTokenVerifier(@Value("${GOOGLE_CLIENT_ID:}") String clientId) {
        this(clientId, NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(5)).build()).build());
    }

    GoogleTokenVerifier(String clientId, NimbusJwtDecoder decoder) {
        this.clientId = clientId.trim();
        this.decoder = decoder;
        decoder.setJwtValidator(JwtValidators.createDefault());
    }

    public boolean configured() { return clientId.matches("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com"); }
    public String clientId() { return configured() ? clientId : null; }
    public void requireConfigured() {
        if (!configured()) throw new AppException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE, "Đăng nhập Google tạm chưa khả dụng. Vui lòng thử lại sau");
    }

    public GoogleIdentity verify(String credential, String nonce) {
        requireConfigured();
        if (nonce == null || !nonce.matches("[a-f0-9]{64}")) throw invalid();
        try {
            Jwt jwt = decoder.decode(credential);
            String tokenNonce = jwt.getClaimAsString("nonce");
            String azp = jwt.getClaimAsString("azp");
            if ((!"https://accounts.google.com".equals(jwt.getClaimAsString("iss")) && !"accounts.google.com".equals(jwt.getClaimAsString("iss")))
                    || jwt.getAudience() == null || !jwt.getAudience().contains(clientId) || (azp != null && !clientId.equals(azp))
                    || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())
                    || jwt.getSubject() == null || jwt.getSubject().isBlank() || jwt.getSubject().length() > 255
                    || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
                    || tokenNonce == null || !MessageDigest.isEqual(tokenNonce.getBytes(StandardCharsets.UTF_8), nonce.getBytes(StandardCharsets.UTF_8))) {
                throw invalid();
            }
            String email = RegistrationOtpService.gmail(jwt.getClaimAsString("email"));
            String name = jwt.getClaimAsString("name");
            if (name == null || name.isBlank()) name = email.split("@")[0];
            return new GoogleIdentity(jwt.getSubject(), email, name.strip().substring(0, Math.min(name.strip().length(), 100)));
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    public record GoogleIdentity(String subject, String email, String fullName) {}
    private AppException invalid() { return new AppException(ErrorCode.UNAUTHENTICATED, "Chứng thực Google không hợp lệ hoặc hết hạn. Vui lòng đăng nhập lại bằng Google"); }
}
