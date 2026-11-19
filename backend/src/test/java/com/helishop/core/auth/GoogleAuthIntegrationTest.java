package com.helishop.core.auth;

import com.fasterxml.jackson.databind.*;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.*;
import com.helishop.core.modules.auth.dto.*;
import com.helishop.core.modules.auth.service.*;
import com.helishop.core.modules.auth.service.GoogleTokenVerifier.GoogleIdentity;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.*;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties = "spring.data.redis.database=15")
@EnabledIf("redisAvailable")
class GoogleAuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired GoogleAuthService service;
    @Autowired StringRedisTemplate redis;
    @MockBean GoogleTokenVerifier verifier;
    @MockBean RefreshTokenService refresh;
    GoogleIdentity identity;
    static boolean redisAvailable() {
        try (Socket socket = new Socket()) { socket.connect(new InetSocketAddress("127.0.0.1", 6379), 1000); return true; }
        catch (java.io.IOException exception) { return false; }
    }
    @BeforeEach void setup() {
        String suffix = UUID.randomUUID().toString();
        identity = new GoogleIdentity(suffix, "google-" + suffix + "@gmail.com", "Verified Google Name");
        when(verifier.configured()).thenReturn(true); when(verifier.clientId()).thenReturn("test-client.apps.googleusercontent.com");
        when(verifier.verify(eq("verified-credential"), anyString())).thenReturn(identity);
    }
    record Proof(Cookie cookie, String ticket, String status) {}
    Cookie challenge() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/google/challenge").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        String nonce = json.readTree(result.getResponse().getContentAsString()).at("/data/nonce").asText();
        String header = result.getResponse().getHeader("Set-Cookie");
        assertTrue(header.contains("HttpOnly")); assertTrue(header.contains("SameSite=Lax")); assertTrue(header.contains(nonce));
        return new Cookie("helishop_google_nonce", nonce);
    }
    Proof begin() throws Exception {
        Cookie cookie = challenge();
        var result = mvc.perform(post("/api/v1/auth/google").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"verified-credential\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString()).get("data");
        assertFalse(data.has("auth"));
        return new Proof(cookie, data.get("ticket").asText(), data.get("status").asText());
    }
    JsonNode finish(Proof proof, Map<String,Object> fields, int expected) throws Exception {
        var body = new HashMap<>(fields); body.put("ticket", proof.ticket());
        var response = mvc.perform(post("/api/v1/auth/google/complete").cookie(proof.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(expected)).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("data");
    }
    User existing(UserRole role) {
        return users.saveAndFlush(User.builder().email(identity.email()).fullName("Existing Name").passwordHash(encoder.encode("ExistingPass42!"))
                .phone("0901234567").role(role).status(UserStatus.ACTIVE).build());
    }
    @Test void googleRegistrationNeedsRoleAndNoPasswordOrSeparateOtp() throws Exception {
        Proof proof = begin(); assertEquals("NEW_ACCOUNT", proof.status()); assertFalse(users.existsByEmail(identity.email()));
        finish(proof, Map.of(), 400);
        var result = finish(proof, Map.of("role", "ROLE_SELLER"), 200);
        User saved = users.findByGoogleSubject(identity.subject()).orElseThrow();
        assertNull(saved.getPasswordHash()); assertEquals(UserRole.ROLE_SELLER, saved.getRole());
        assertEquals(saved.getId(), result.at("/auth/userId").asLong()); verify(refresh).saveRefreshToken(eq(identity.email()), anyString());
        finish(proof, Map.of("role", "ROLE_SELLER"), 401);
    }
    @Test void cannotRegisterAdminOrChooseAnotherEmailThroughRequest() throws Exception {
        Proof proof = begin(); finish(proof, Map.of("role", "ROLE_ADMIN"), 403); assertFalse(users.existsByEmail(identity.email()));
        finish(proof, Map.of("role", "ROLE_CUSTOMER", "email", "attacker@gmail.com", "googleSubject", "attacker"), 200);
        assertFalse(users.existsByEmail("attacker@gmail.com")); assertEquals(UserRole.ROLE_CUSTOMER, users.findByGoogleSubject(identity.subject()).orElseThrow().getRole());
    }
    @Test void existingAccountRequiresPasswordAndKeepsIdentityRoleProfileAndPassword() throws Exception {
        User old = existing(UserRole.ROLE_CUSTOMER); String hash = old.getPasswordHash();
        Proof proof = begin(); assertEquals("LINK_REQUIRED", proof.status());
        finish(proof, Map.of("role", "ROLE_SELLER"), 401);
        finish(proof, Map.of("password", "wrong"), 401);
        var result = finish(proof, Map.of("password", "ExistingPass42!", "role", "ROLE_SELLER"), 200);
        User linked = users.findByGoogleSubject(identity.subject()).orElseThrow();
        assertEquals(old.getId(), linked.getId()); assertEquals(UserRole.ROLE_CUSTOMER, linked.getRole()); assertEquals(hash, linked.getPasswordHash());
        assertEquals("Existing Name", linked.getFullName()); assertEquals("0901234567", linked.getPhone()); assertEquals(old.getId(), result.at("/auth/userId").asLong());
        verify(refresh, times(1)).saveRefreshToken(eq(identity.email()), anyString());
    }
    @Test void oldAdminCannotBeLinkedWithoutPasswordAndRetainsAdminAfterProof() throws Exception {
        User admin = existing(UserRole.ROLE_ADMIN); Proof proof = begin();
        finish(proof, Map.of("password", "wrong"), 401); verifyNoInteractions(refresh);
        var result = finish(proof, Map.of("password", "ExistingPass42!"), 200);
        assertEquals("ROLE_ADMIN", result.at("/auth/role").asText()); assertEquals(admin.getId(), result.at("/auth/userId").asLong());
    }
    @Test void fiveBadPasswordsRevokeLinkTicketInRealRedis() throws Exception {
        User old = existing(UserRole.ROLE_CUSTOMER); Proof proof = begin();
        for (int i=0; i<5; i++) finish(proof, Map.of("password", "wrong"), 401);
        finish(proof, Map.of("password", "ExistingPass42!"), 401);
        assertNull(users.findById(old.getId()).orElseThrow().getGoogleSubject()); verifyNoInteractions(refresh);
    }
    @Test void ticketCannotBeUsedByAnotherBrowserCookie() throws Exception {
        Proof proof = begin(); Cookie other = challenge();
        finish(new Proof(other, proof.ticket(), proof.status()), Map.of("role", "ROLE_CUSTOMER"), 401);
        assertFalse(users.existsByEmail(identity.email())); finish(proof, Map.of("role", "ROLE_CUSTOMER"), 200);
    }
    @Test void googleNonceIsConsumedOnceInRealRedis() throws Exception {
        Cookie cookie = challenge();
        String body = "{\"credential\":\"verified-credential\"}";
        mvc.perform(post("/api/v1/auth/google").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/google").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
    }
    @Test void linkedAccountSignsInDirectlyAndBlockedAccountIsDenied() throws Exception {
        User linked = existing(UserRole.ROLE_CUSTOMER); linked.setGoogleSubject(identity.subject()); users.saveAndFlush(linked);
        Cookie cookie = challenge();
        mvc.perform(post("/api/v1/auth/google").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"verified-credential\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("AUTHENTICATED")).andExpect(jsonPath("$.data.auth.userId").value(linked.getId()));
        linked.setStatus(UserStatus.BLOCKED); users.saveAndFlush(linked);
        mvc.perform(post("/api/v1/auth/google").cookie(challenge()).contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"verified-credential\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void lockedAfterVerificationAndConflictingGoogleIdentityAreDenied() throws Exception {
        User old = existing(UserRole.ROLE_SELLER); Proof proof = begin();
        old.setStatus(UserStatus.BLOCKED); users.saveAndFlush(old); finish(proof, Map.of("password", "ExistingPass42!"), 401);
        old.setStatus(UserStatus.ACTIVE); old.setGoogleSubject("different-subject"); users.saveAndFlush(old);
        mvc.perform(post("/api/v1/auth/google").cookie(challenge()).contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"verified-credential\"}"))
                .andExpect(status().isForbidden()); verifyNoInteractions(refresh);
    }
    @Test void invalidCredentialAndMissingCookieNeverCreateUser() throws Exception {
        when(verifier.verify(eq("invalid"), anyString())).thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));
        mvc.perform(post("/api/v1/auth/google").cookie(challenge()).contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"invalid\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/google/complete").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\""+"a".repeat(64)+"\",\"role\":\"ROLE_CUSTOMER\"}"))
                .andExpect(status().isUnauthorized());
        assertFalse(users.existsByEmail(identity.email())); verifyNoInteractions(refresh);
    }
    @Test void expiredPendingTicketFailsClosed() throws Exception {
        Proof proof = begin();
        // Only this test's ticket in logical DB 15; never flush a shared database.
        String key = "auth:google:pending:" + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(proof.ticket().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(redis.getExpire(key) > 0); redis.expire(key, Duration.ZERO);
        finish(proof, Map.of("role", "ROLE_CUSTOMER"), 401); assertFalse(users.existsByEmail(identity.email()));
    }
    @Test void concurrentUseOfOneTicketCreatesExactlyOneAccount() throws Exception {
        Proof proof = begin(); var executor = Executors.newFixedThreadPool(2); var gate = new CountDownLatch(1);
        try {
            Callable<Boolean> call = () -> { gate.await(); try { service.complete(new GoogleCompleteRequest(proof.ticket(), UserRole.ROLE_CUSTOMER, null), proof.cookie().getValue()); return true; }
                catch (AppException exception) { assertTrue(Set.of(ErrorCode.UNAUTHENTICATED, ErrorCode.OPTIMISTIC_LOCK_CONFLICT).contains(exception.getErrorCode())); return false; } };
            var first = executor.submit(call); var second = executor.submit(call); gate.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, users.findAll().stream().filter(user -> user.getEmail().equals(identity.email())).count());
        } finally { executor.shutdownNow(); }
    }
}
