package com.helishop.core.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.service.CheckoutService;
import com.helishop.core.modules.product.dto.*;
import com.helishop.core.modules.product.entity.*;
import com.helishop.core.modules.product.repository.*;
import com.helishop.core.modules.product.service.*;
import com.helishop.core.modules.user.entity.*;
import com.helishop.core.modules.user.repository.*;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:seller_product_tests;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=WARN"})
@AutoConfigureMockMvc
class SellerProductIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtils jwt;
    @Autowired SellerProductService service;
    @Autowired ProductService catalog;
    @Autowired CheckoutService checkout;
    @Autowired ProductRepository products;
    @Autowired ProductSkuRepository skus;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired UserAddressRepository addresses;
    @MockBean RefreshTokenService refresh;
    @MockBean StringRedisTemplate redis;
    @MockBean RedisCartService cart;
    @MockBean CacheManager caches;
    User seller, stranger;
    Shop shop;
    Long category;
    String suffix;

    @BeforeEach void setup() {
        when(caches.getCache("products")).thenReturn(new ConcurrentMapCache("products"));
        suffix = UUID.randomUUID().toString();
        seller = user(UserRole.ROLE_SELLER); stranger = user(UserRole.ROLE_SELLER);
        shop = shops.saveAndFlush(Shop.builder().owner(seller).shopName("Mine-" + suffix).status(ShopStatus.ACTIVE).build());
        shops.saveAndFlush(Shop.builder().owner(stranger).shopName("Other-" + suffix).status(ShopStatus.ACTIVE).build());
        category = categories.saveAndFlush(Category.builder().name("Category").slug(suffix).build()).getId();
    }
    User user(UserRole role) { return users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.test").fullName("Test").passwordHash("hash").role(role).status(UserStatus.ACTIVE).build()); }
    String token(User user) { return "Bearer " + jwt.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name(), user.getFullName()); }
    SellerProductRequest metadata(ProductStatus status) { return new SellerProductRequest("Product-" + suffix, category, "Description", null, status); }
    SellerSkuRequest sku(String code, String color, int stock, String price, Long version) { return new SellerSkuRequest(code, new BigDecimal(price), null, stock, Map.of("Màu", color), null, version); }
    ProductResponse create(User owner) { return service.create(owner.getId(), new SellerProductCreateRequest(metadata(ProductStatus.ACTIVE), List.of(sku(UUID.randomUUID().toString(), "Đỏ", 10, "100000", null)))); }
    void postSku(Long id, SellerSkuRequest request, int status) throws Exception {
        mvc.perform(post("/api/v1/seller/products/"+id+"/skus").header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().is(status));
    }

    @Test void createsProductWithSkusAtomicallyDerivesOwnershipPriceStockAndPublicDetail() throws Exception {
        var request = new SellerProductCreateRequest(metadata(ProductStatus.ACTIVE), List.of(sku("ONE-"+suffix,"Đỏ",4,"200000",null),sku("TWO-"+suffix,"Xanh",7,"150000",null)));
        String body = json.writeValueAsString(request).replace("\"product\":{", "\"shopId\":999,\"ownerId\":999,\"price\":1,\"product\":{\"rating\":5,");
        var response = mvc.perform(post("/api/v1/seller/products").header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.shopId").value(shop.getId()))
                .andExpect(jsonPath("$.data.price").value(150000)).andExpect(jsonPath("$.data.stockQuantity").value(11))
                .andExpect(jsonPath("$.data.skus.length()").value(2)).andExpect(jsonPath("$.data.skus[0].version").value(0)).andReturn();
        long id = json.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(get("/api/v1/products/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.skus.length()").value(2));
    }

    @Test void ownListIncludesHiddenDraftsIsPaginatedFilteredAndNeverLeaksOtherShops() throws Exception {
        create(seller); create(stranger);
        service.create(seller.getId(), new SellerProductCreateRequest(metadata(ProductStatus.DRAFT), List.of(sku("DRAFT-"+suffix,"Đỏ",0,"1",null))));
        mvc.perform(get("/api/v1/seller/products").param("shopId","999").param("size","1").param("page","2").header("Authorization",token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2)).andExpect(jsonPath("$.data.items.length()").value(1)).andExpect(jsonPath("$.data.items[0].shopId").value(shop.getId()));
        mvc.perform(get("/api/v1/seller/products").param("status","DRAFT").param("keyword",suffix).header("Authorization",token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        for (String query : List.of("?page=0", "?size=51", "?status=INVALID")) mvc.perform(get("/api/v1/seller/products"+query).header("Authorization",token(seller))).andExpect(status().isBadRequest());
    }

    @Test void authenticationDatabaseRoleUserAndShopStatusAreEnforced() throws Exception {
        mvc.perform(get("/api/v1/seller/products")).andExpect(status().isUnauthorized());
        for (UserRole role : List.of(UserRole.ROLE_CUSTOMER, UserRole.ROLE_ADMIN)) mvc.perform(get("/api/v1/seller/products").header("Authorization",token(user(role)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/seller/products").header("Authorization",token(user(UserRole.ROLE_SELLER)))).andExpect(status().isNotFound());
        String oldToken = token(seller);
        seller.setRole(UserRole.ROLE_CUSTOMER); users.saveAndFlush(seller);
        mvc.perform(get("/api/v1/seller/products").header("Authorization",oldToken)).andExpect(status().isForbidden());
        seller.setRole(UserRole.ROLE_SELLER); seller.setStatus(UserStatus.BLOCKED); users.saveAndFlush(seller);
        mvc.perform(get("/api/v1/seller/products").header("Authorization",oldToken)).andExpect(status().isForbidden());
        seller.setStatus(UserStatus.ACTIVE); users.saveAndFlush(seller);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        mvc.perform(get("/api/v1/seller/products").header("Authorization",oldToken)).andExpect(status().isForbidden());
        String legacy = json.writeValueAsString(Map.of("shopId", shop.getId(),"categoryId",category,"name","Forbidden"));
        mvc.perform(post("/api/v1/products").header("Authorization",oldToken).contentType(MediaType.APPLICATION_JSON).content(legacy)).andExpect(status().isForbidden());
    }

    @Test void anotherSellerCannotReadEditOrAttachSkuToProductAndSkuMustBelongToPath() throws Exception {
        var foreign = create(stranger); var own = create(seller);
        String root = "/api/v1/seller/products/"+foreign.getId();
        mvc.perform(get(root).header("Authorization",token(seller))).andExpect(status().isForbidden());
        mvc.perform(put(root).header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(metadata(ProductStatus.INACTIVE)))).andExpect(status().isForbidden());
        postSku(foreign.getId(),sku("FOREIGN-"+suffix,"Đen",1,"1",null),403);
        mvc.perform(put("/api/v1/seller/products/"+own.getId()+"/skus/"+foreign.getSkus().getFirst().getId()).header("Authorization",token(seller))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(sku("FORGE-"+suffix,"Đen",1,"1",0L)))).andExpect(status().isNotFound());
    }

    @Test void rejectsInvalidMetadataPricesStocksImageUrlsAndAttributesWithoutPartialSave() throws Exception {
        long count = products.count();
        var good = new SellerProductCreateRequest(metadata(ProductStatus.ACTIVE),List.of(sku("INVALID-"+suffix,"Đỏ",1,"100",null)));
        String body = json.writeValueAsString(good);
        for (String invalid : List.of(body.replace("\"stockQuantity\":1","\"stockQuantity\":-1"), body.replace("\"price\":100","\"price\":0"),
                body.replace("\"price\":100","\"price\":100.001"), body.replace("\"originalPrice\":null","\"originalPrice\":1"),
                body.replace("\"mainImageUrl\":null","\"mainImageUrl\":\"javascript:alert(1)\""), body.replace("\"skus\":[", "\"skus\":[null,"),
                body.replace("\"categoryId\":"+category,"\"categoryId\":9999999"),body.replace("Product-"+suffix," "), "{}")) {
            int status = mvc.perform(post("/api/v1/seller/products").header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(invalid)).andReturn().getResponse().getStatus();
            assertThat(status).isIn(400,404);
        }
        assertThat(products.count()).isEqualTo(count);
    }

    @Test void duplicateCodesAndVariantCombinationsRollbackAndAllowRetry() throws Exception {
        var product = create(seller); String code = product.getSkus().getFirst().getSkuCode();
        postSku(product.getId(),sku(code.toLowerCase(Locale.ROOT),"Xanh",1,"1",null),409);
        postSku(product.getId(),sku("DUP-"+suffix,"Đỏ",1,"1",null),409);
        postSku(product.getId(),new SellerSkuRequest("BADKEY-"+suffix,BigDecimal.ONE,null,1,Map.of("Size","XL"),null,null),400);
        long count = products.count();
        assertThatThrownBy(() -> service.create(seller.getId(),new SellerProductCreateRequest(metadata(ProductStatus.ACTIVE),List.of(sku("NEW-"+suffix,"Đen",1,"1",null),sku(code,"Xanh",1,"1",null))))).isInstanceOf(AppException.class);
        assertThat(products.count()).isEqualTo(count);
        postSku(product.getId(),sku("GOOD-"+suffix,"Xanh",3,"50000",null),201);
        assertThat(service.detail(seller.getId(),product.getId()).getStockQuantity()).isEqualTo(13);
    }

    @Test void skuEditsPreserveIdsRecomputeAggregateAndEvictCacheAfterCommitIncludingHide() throws Exception {
        var product = create(seller);
        var first = product.getSkus().getFirst();
        assertThat(catalog.getById(product.getId()).getPrice()).isEqualByComparingTo("100000");
        var updated = service.saveSku(seller.getId(),product.getId(),first.getId(),sku(first.getSkuCode(),"Đỏ",7,"80000",first.getVersion()));
        assertThat(updated.getSkus().getFirst().getId()).isEqualTo(first.getId());
        assertThat(updated.getSkus().getFirst().getVersion()).isGreaterThan(first.getVersion());
        assertThat(catalog.getById(product.getId()).getPrice()).isEqualByComparingTo("80000");
        assertThat(catalog.getById(product.getId()).getStockQuantity()).isEqualTo(7);
        String legacy = json.writeValueAsString(Map.of("shopId",shop.getId(),"categoryId",category,"name","Legacy","stockQuantity",999,"price",1));
        mvc.perform(put("/api/v1/products/"+product.getId()).header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(legacy)).andExpect(status().isBadRequest());
        service.update(seller.getId(),product.getId(),metadata(ProductStatus.INACTIVE));
        mvc.perform(get("/api/v1/products/"+product.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/seller/products/"+product.getId()).header("Authorization",token(seller))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test void staleVersionAfterRealCheckoutCannotRestoreSoldInventoryAndFreshVersionCanRestock() throws Exception {
        var product = create(seller); var first = product.getSkus().getFirst();
        User buyer = user(UserRole.ROLE_CUSTOMER); var request = checkoutRequest(buyer,first.getId());
        var order = checkout.checkout(buyer.getId(),request,"seller-checkout-"+suffix).getFirst();
        mvc.perform(put("/api/v1/seller/products/"+product.getId()+"/skus/"+first.getId()).header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(sku(first.getSkuCode(),"Đỏ",10,"100000",first.getVersion())))).andExpect(status().isConflict());
        assertThat(skus.findById(first.getId()).orElseThrow().getStockQuantity()).isEqualTo(9);
        var latest = service.detail(seller.getId(),product.getId()).getSkus().getFirst();
        service.saveSku(seller.getId(),product.getId(),latest.getId(),sku(latest.getSkuCode(),"Đỏ",20,"50000",latest.getVersion()));
        assertThat(order.getItems().getFirst().getPriceAtPurchase()).isEqualByComparingTo("100000");
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(20);
        service.update(seller.getId(),product.getId(),metadata(ProductStatus.INACTIVE));
        assertThatThrownBy(() -> checkout.checkout(buyer.getId(),request,"hidden-checkout-"+suffix)).isInstanceOf(AppException.class);
        mvc.perform(post("/api/v1/orders/checkout").header("Authorization",token(buyer)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("paymentMethod","COD","shippingAddressSnapshot","Test address","items",List.of(Map.of("skuId",first.getId(),"quantity",1))))))
                .andExpect(status().isBadRequest());
    }

    @Test void parallelEditsWithSameVersionAllowOneWinner() throws Exception {
        var product = create(seller); var first = product.getSkus().getFirst();
        assertThat(race(() -> service.saveSku(seller.getId(),product.getId(),first.getId(),sku(first.getSkuCode(),"Đỏ",7,"50000",first.getVersion())),
                () -> service.saveSku(seller.getId(),product.getId(),first.getId(),sku(first.getSkuCode(),"Đỏ",8,"60000",first.getVersion())))).containsExactlyInAnyOrder(200,409);
        assertThat(service.detail(seller.getId(),product.getId()).getStockQuantity()).isIn(7,8);
    }

    @Test void checkoutAndEditUseCompatibleLocksAndNeverOverwritePurchasedStock() throws Exception {
        var product = create(seller); var first = product.getSkus().getFirst(); User buyer = user(UserRole.ROLE_CUSTOMER);
        var request = checkoutRequest(buyer,first.getId());
        var outcomes = race(() -> service.saveSku(seller.getId(),product.getId(),first.getId(),sku(first.getSkuCode(),"Đỏ",6,"100000",first.getVersion())),
                () -> checkout.checkout(buyer.getId(),request,"parallel-checkout-"+suffix));
        assertThat(outcomes).allMatch(code -> code == 200 || code == 409).contains(200);
        var actual = service.detail(seller.getId(),product.getId());
        assertThat(actual.getStockQuantity()).isEqualTo(actual.getSkus().getFirst().getStockQuantity()).isIn(5,9);
    }

    @Test void concurrentGlobalSkuCodeCollisionRollsBackLosingProduct() throws Exception {
        String code = "RACE-"+suffix;
        var request = new SellerProductCreateRequest(metadata(ProductStatus.ACTIVE),List.of(sku(code,"Đỏ",1,"1",null)));
        long count = products.count();
        assertThat(race(() -> service.create(seller.getId(),request), () -> service.create(stranger.getId(),request))).containsExactlyInAnyOrder(200,409);
        assertThat(products.count()).isEqualTo(count+1);
    }

    CheckoutBatchRequest checkoutRequest(User buyer, Long skuId) {
        Long address = addresses.saveAndFlush(UserAddress.builder().user(buyer).recipientName("Buyer").phone("0901234567").streetAddress("1 Test").ward("Ward").district("District").provinceCity("City").isDefault(true).build()).getId();
        return CheckoutBatchRequest.builder().addressId(address).shippingMethod("standard").paymentMethod(PaymentMethod.COD).items(List.of(CheckoutRequest.ItemRequest.builder().skuId(skuId).quantity(1).build())).build();
    }
    interface Action { Object run(); }
    List<Integer> race(Action first, Action second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1), ready = new CountDownLatch(2);
            List<Future<Integer>> calls = new ArrayList<>();
            for (Action action : List.of(first,second)) calls.add(pool.submit(() -> { ready.countDown(); if (!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); try { action.run(); return 200; } catch (AppException error) { return error.getErrorCode().getStatusCode().value(); } }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(calls.get(0).get(20,TimeUnit.SECONDS),calls.get(1).get(20,TimeUnit.SECONDS));
        }
    }
}
