package com.helishop.core.security;

import com.helishop.core.common.constants.*;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc
class AdminAccountIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired UserRepository users;
    @MockBean RefreshTokenService refresh;
    @MockBean StringRedisTemplate redis;
    User admin, buyer, seller;
    String prefix;
    @BeforeEach void setup() {
        prefix = UUID.randomUUID().toString();
        admin = user(UserRole.ROLE_ADMIN); buyer = user(UserRole.ROLE_CUSTOMER); seller = user(UserRole.ROLE_SELLER);
    }
    User user(UserRole role) {
        return users.saveAndFlush(User.builder().email(prefix + role + UUID.randomUUID() + "@gmail.com").fullName("Account " + prefix)
                .passwordHash("never-return-this-hash").role(role).status(UserStatus.ACTIVE).build());
    }
    String token(User user) { return "Bearer " + jwt.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name(), user.getFullName(), user.getAuthVersion()); }
    void change(User target, String expected, String status, int result) throws Exception {
        mvc.perform(patch("/api/v1/admin/users/" + target.getId() + "/status").header("Authorization", token(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedStatus\":\"" + expected + "\",\"status\":\"" + status + "\"}"))
                .andExpect(status().is(result));
    }
    @Test void listIsPrivateAndNeverLeaksSecretsOrAdmins() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        for (var user : new User[]{buyer,seller}) mvc.perform(get("/api/v1/admin/users").header("Authorization", token(user))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", token(admin)).param("keyword", prefix))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist()).andExpect(jsonPath("$.data.items[0].authVersion").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].phone").doesNotExist()).andExpect(jsonPath("$.data.items[0].addresses").doesNotExist());
    }
    @Test void filtersAndValidationApplyOnServer() throws Exception {
        mvc.perform(get("/api/v1/admin/users").header("Authorization", token(admin)).param("keyword", prefix).param("role", "ROLE_SELLER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.items[0].id").value(seller.getId()));
        for (String query : new String[]{"page=0", "size=51", "role=ROLE_ADMIN", "status=UNKNOWN"}) {
            mvc.perform(get("/api/v1/admin/users?" + query).header("Authorization", token(admin))).andExpect(status().isBadRequest());
        }
    }
    @Test void blockAndUnblockPermanentlyInvalidateOldAccessTokens() throws Exception {
        String oldToken = token(buyer);
        mvc.perform(get("/api/v1/users/me").header("Authorization", oldToken)).andExpect(status().isOk());
        change(buyer, "ACTIVE", "BLOCKED", 200);
        verify(refresh).deleteByUserEmail(buyer.getEmail());
        mvc.perform(get("/api/v1/users/me").header("Authorization", oldToken)).andExpect(status().isUnauthorized());
        change(buyer, "BLOCKED", "ACTIVE", 200);
        mvc.perform(get("/api/v1/users/me").header("Authorization", oldToken)).andExpect(status().isUnauthorized());
        User updated = users.findById(buyer.getId()).orElseThrow();
        assertEquals(2L, updated.getAuthVersion());
        mvc.perform(get("/api/v1/users/me").header("Authorization", token(updated))).andExpect(status().isOk());
    }
    @Test void cannotBlockSelfOrOtherAdminsOrChangeRole() throws Exception {
        change(admin, "ACTIVE", "BLOCKED", 400);
        change(user(UserRole.ROLE_ADMIN), "ACTIVE", "BLOCKED", 404);
        change(buyer, "ACTIVE", "INACTIVE", 400);
        mvc.perform(patch("/api/v1/admin/users/" + buyer.getId() + "/status").header("Authorization", token(seller))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedStatus\":\"ACTIVE\",\"status\":\"BLOCKED\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertEquals(UserRole.ROLE_CUSTOMER, users.findById(buyer.getId()).orElseThrow().getRole());
    }
    @Test void staleUpdateConflictsAndRetryHasNoNewSideEffects() throws Exception {
        change(seller, "ACTIVE", "BLOCKED", 200);
        change(seller, "ACTIVE", "BLOCKED", 200);
        verify(refresh, times(1)).deleteByUserEmail(seller.getEmail());
        change(seller, "INACTIVE", "ACTIVE", 409);
        assertEquals(UserStatus.BLOCKED, users.findById(seller.getId()).orElseThrow().getStatus());
    }
    @Test void staleOrIncorrectAdminClaimsCannotBypassDatabaseRole() throws Exception {
        String token = "Bearer " + jwt.generateAccessToken(buyer.getEmail(), buyer.getId(), "ROLE_ADMIN", buyer.getFullName());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", token)).andExpect(status().isForbidden());
        admin.setStatus(UserStatus.BLOCKED); users.saveAndFlush(admin);
        mvc.perform(get("/api/v1/admin/users").header("Authorization", token(admin))).andExpect(status().isUnauthorized());
    }
    @Test void mismatchedOrDeletedIdentityIsNotAuthenticated() throws Exception {
        String token = "Bearer " + jwt.generateAccessToken("other@gmail.com", buyer.getId(), buyer.getRole().name(), "Other");
        mvc.perform(get("/api/v1/users/me").header("Authorization", token)).andExpect(status().isUnauthorized());
        String deleted = token(buyer); users.deleteById(buyer.getId());
        mvc.perform(get("/api/v1/users/me").header("Authorization", deleted)).andExpect(status().isUnauthorized());
    }
}
