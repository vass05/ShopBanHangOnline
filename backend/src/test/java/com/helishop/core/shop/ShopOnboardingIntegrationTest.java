package com.helishop.core.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.user.dto.OpenShopRequest;
import com.helishop.core.modules.user.entity.*;
import com.helishop.core.modules.user.repository.*;
import com.helishop.core.modules.user.service.ShopService;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shop_tests;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"
})
@AutoConfigureMockMvc
class ShopOnboardingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ShopService service;
    @MockBean RefreshTokenService refresh;
    @MockBean StringRedisTemplate redis;

    User user(UserRole role) {
        return users.saveAndFlush(User.builder().email(UUID.randomUUID() + "@example.test").passwordHash("hash")
                .fullName("Shop test").role(role).status(UserStatus.ACTIVE).build());
    }
    String token(User user) { return "Bearer " + jwt.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name(), user.getFullName()); }
    String name() { return "Shop-" + UUID.randomUUID(); }
    String body(String name) throws Exception { return json.writeValueAsString(Map.of("shopName", name)); }

    @Test void opensAndReadsOnlyTokenOwnersShopWithServerStatus() throws Exception {
        User seller = user(UserRole.ROLE_SELLER), other = user(UserRole.ROLE_SELLER);
        String shopName = name();
        mvc.perform(get("/api/v1/shops/me").header("Authorization", token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(post("/api/v1/shops/me").header("Authorization", token(seller)).param("ownerId", other.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "shopName", "  " + shopName + "  ", "description", "  Mô tả shop  ", "ownerId", other.getId(), "status", "BANNED"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ownerId").value(seller.getId())).andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.shopName").value(shopName)).andExpect(jsonPath("$.data.description").value("Mô tả shop"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
        assertThat(shops.findByOwnerId(seller.getId())).isPresent();
        mvc.perform(get("/api/v1/shops/me").header("Authorization", token(seller)).param("ownerId", other.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.shopName").value(shopName));
        mvc.perform(get("/api/v1/shops/me").header("Authorization", token(other)).param("ownerId", seller.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test void rejectsAnonymousCustomerAdminAndStaleSellerClaims() throws Exception {
        mvc.perform(get("/api/v1/shops/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/shops/me").contentType(MediaType.APPLICATION_JSON).content(body(name())))
                .andExpect(status().isUnauthorized());
        for (UserRole role : List.of(UserRole.ROLE_CUSTOMER, UserRole.ROLE_ADMIN)) {
            User user = user(role);
            mvc.perform(get("/api/v1/shops/me").header("Authorization", token(user))).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/shops/me").header("Authorization", token(user)).contentType(MediaType.APPLICATION_JSON).content(body(name())))
                    .andExpect(status().isForbidden());
            assertThat(shops.findByOwnerId(user.getId())).isEmpty();
        }
        User stale = user(UserRole.ROLE_SELLER);
        String staleToken = token(stale);
        stale.setRole(UserRole.ROLE_CUSTOMER); users.saveAndFlush(stale);
        mvc.perform(post("/api/v1/shops/me").header("Authorization", staleToken).contentType(MediaType.APPLICATION_JSON).content(body(name())))
                .andExpect(status().isForbidden());
        for (UserStatus status : List.of(UserStatus.INACTIVE, UserStatus.BLOCKED)) {
            User blocked = user(UserRole.ROLE_SELLER);
            String token = token(blocked);
            blocked.setStatus(status); users.saveAndFlush(blocked);
            mvc.perform(get("/api/v1/shops/me").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/shops/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body(name())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void rejectsInvalidNamesDescriptionAndMalformedBodyWithoutSaving() throws Exception {
        User seller = user(UserRole.ROLE_SELLER);
        for (String invalid : List.of("", " ", "ab", " ab ", "x".repeat(151))) {
            mvc.perform(post("/api/v1/shops/me").header("Authorization", token(seller)).contentType(MediaType.APPLICATION_JSON).content(body(invalid)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1010));
        }
        for (String invalid : List.of("{}", "{", json.writeValueAsString(Map.of("shopName", name(), "description", "x".repeat(2001))))) {
            mvc.perform(post("/api/v1/shops/me").header("Authorization", token(seller)).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        assertThat(shops.findByOwnerId(seller.getId())).isEmpty();
    }

    @Test void oneShopPerSellerAndCaseInsensitiveNamesIncludingInactiveShops() throws Exception {
        User seller = user(UserRole.ROLE_SELLER), other = user(UserRole.ROLE_SELLER);
        String name = name();
        service.open(seller.getId(), OpenShopRequest.builder().shopName(name).build());
        for (ShopStatus status : List.of(ShopStatus.ACTIVE, ShopStatus.INACTIVE, ShopStatus.BANNED)) {
            Shop shop = shops.findByOwnerId(seller.getId()).orElseThrow();
            shop.setStatus(status); shops.saveAndFlush(shop);
            mvc.perform(post("/api/v1/shops/me").header("Authorization", token(seller)).contentType(MediaType.APPLICATION_JSON).content(body(name())))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(1016));
            mvc.perform(get("/api/v1/shops/me").header("Authorization", token(seller)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(status.name()));
        }
        mvc.perform(post("/api/v1/shops/me").header("Authorization", token(other)).contentType(MediaType.APPLICATION_JSON).content(body(name.toUpperCase(Locale.ROOT))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(1017));
        assertThat(shops.findByOwnerId(other.getId())).isEmpty();
        mvc.perform(post("/api/v1/shops/me").header("Authorization", token(other)).contentType(MediaType.APPLICATION_JSON).content(body(name())))
                .andExpect(status().isCreated());
    }

    @Test void concurrentRequestsForSameOwnerCreateExactlyOneShop() throws Exception {
        User seller = user(UserRole.ROLE_SELLER);
        List<Integer> results = race(List.of(seller, seller, seller, seller), false);
        assertThat(results).containsExactlyInAnyOrder(201, 409, 409, 409);
        assertThat(shops.findByOwnerId(seller.getId())).isPresent();
    }

    @Test void concurrentSellersCannotClaimSameNameAndLosingTransactionRollsBack() throws Exception {
        List<User> sellers = List.of(user(UserRole.ROLE_SELLER), user(UserRole.ROLE_SELLER));
        assertThat(race(sellers, true)).containsExactlyInAnyOrder(201, 409);
        assertThat(sellers.stream().filter(seller -> shops.findByOwnerId(seller.getId()).isPresent()).count()).isEqualTo(1);
        User loser = sellers.stream().filter(seller -> shops.findByOwnerId(seller.getId()).isEmpty()).findFirst().orElseThrow();
        assertThat(service.open(loser.getId(), OpenShopRequest.builder().shopName(name()).build()).getOwnerId()).isEqualTo(loser.getId());
    }

    List<Integer> race(List<User> sellers, boolean sameName) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(sellers.size());
        CountDownLatch ready = new CountDownLatch(sellers.size()), start = new CountDownLatch(1);
        String commonName = name();
        try {
            List<Future<Integer>> futures = sellers.stream().map(seller -> pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                try {
                    service.open(seller.getId(), OpenShopRequest.builder().shopName(sameName ? commonName : name()).build());
                    return 201;
                } catch (AppException error) { return error.getErrorCode().getStatusCode().value(); }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
