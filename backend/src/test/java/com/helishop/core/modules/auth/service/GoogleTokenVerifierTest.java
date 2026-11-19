package com.helishop.core.modules.auth.service;

import com.helishop.core.common.exception.AppException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GoogleTokenVerifierTest {
    static RSAKey key;
    static final String CLIENT = "test-client.apps.googleusercontent.com", NONCE = "a".repeat(64);
    GoogleTokenVerifier verifier;
    @BeforeAll static void keys() throws Exception { key = new RSAKeyGenerator(2048).generate(); }
    @BeforeEach void setup() throws Exception { verifier = new GoogleTokenVerifier(CLIENT, NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build()); }
    String token(Map<String, Object> overrides) throws Exception { return token(overrides, key); }
    String token(Map<String, Object> overrides, RSAKey signing) throws Exception {
        var claims = new HashMap<String, Object>();
        claims.put("iss", "https://accounts.google.com"); claims.put("sub", "1234567890"); claims.put("aud", CLIENT);
        claims.put("exp", Date.from(Instant.now().plusSeconds(3600))); claims.put("iat", new Date());
        claims.put("email", "owner@gmail.com"); claims.put("email_verified", true); claims.put("name", "Google Owner"); claims.put("nonce", NONCE);
        claims.putAll(overrides);
        var builder = new JWTClaimsSet.Builder(); claims.forEach(builder::claim);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), builder.build());
        jwt.sign(new RSASSASigner(signing)); return jwt.serialize();
    }
    @Test void genuineSignatureAndVerifiedGmailAreAccepted() throws Exception {
        var identity = verifier.verify(token(Map.of()), NONCE);
        assertEquals("1234567890", identity.subject()); assertEquals("owner@gmail.com", identity.email()); assertEquals("Google Owner", identity.fullName());
    }
    @Test void bothDocumentedGoogleIssuersAccepted() throws Exception { assertNotNull(verifier.verify(token(Map.of("iss", "accounts.google.com")), NONCE)); }
    @Test void forgedSignatureRejected() throws Exception { String value = token(Map.of(), new RSAKeyGenerator(2048).generate()); assertThrows(AppException.class, () -> verifier.verify(value, NONCE)); }
    @Test void wrongAudienceIssuerAzpAndUnverifiedEmailRejected() throws Exception {
        for (var override : List.of(Map.<String,Object>of("aud", "attacker.apps.googleusercontent.com"), Map.<String,Object>of("iss", "https://attacker.test"),
                Map.<String,Object>of("azp", "attacker.apps.googleusercontent.com"), Map.<String,Object>of("email_verified", false))) {
            String value = token(override); assertThrows(AppException.class, () -> verifier.verify(value, NONCE));
        }
    }
    @Test void expiredTokenAndMismatchedNonceRejected() throws Exception {
        String expired = token(Map.of("exp", Date.from(Instant.now().minusSeconds(120))));
        assertThrows(AppException.class, () -> verifier.verify(expired, NONCE));
        String value = token(Map.of()); assertThrows(AppException.class, () -> verifier.verify(value, "b".repeat(64)));
        assertThrows(AppException.class, () -> verifier.verify(value, null));
    }
    @Test void missingRequiredClaimsRejectedWithoutServerError() throws Exception {
        for (String field : List.of("iss", "aud", "sub", "exp", "nonce", "email", "email_verified")) {
            var override = new HashMap<String, Object>(); override.put(field, null);
            String value = token(override); assertThrows(AppException.class, () -> verifier.verify(value, NONCE), field);
        }
    }
    @Test void nonGmailAddressIsNotAcceptedEvenIfGoogleEmailVerified() throws Exception {
        String value = token(Map.of("email", "owner@example.com")); assertThrows(AppException.class, () -> verifier.verify(value, NONCE));
    }
    @Test void malformedCredentialAndDisabledProviderRejected() throws Exception {
        assertThrows(AppException.class, () -> verifier.verify("not-a-jwt", NONCE));
        var disabled = new GoogleTokenVerifier("", NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build());
        assertFalse(disabled.configured()); assertNull(disabled.clientId());
        assertThrows(AppException.class, () -> disabled.verify(token(Map.of()), NONCE));
    }
}
