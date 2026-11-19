package com.helishop.core.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.order.service.*;
import com.helishop.core.modules.payment.service.PaymentService;
import com.helishop.core.modules.payment.config.VnPayConfig;
import com.helishop.core.modules.payment.util.VnPayUtils;
import com.helishop.core.modules.product.entity.*;
import com.helishop.core.modules.product.repository.*;
import com.helishop.core.modules.product.service.ProductService;
import com.helishop.core.modules.user.entity.*;
import com.helishop.core.modules.user.repository.*;
import com.helishop.core.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:seller_fulfillment;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=WARN"})
@AutoConfigureMockMvc
class SellerOrderFulfillmentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtils jwt;
    @Autowired SellerOrderService service;
    @Autowired CheckoutService checkout;
    @Autowired ProductService catalog;
    @Autowired PaymentService payment;
    @Autowired VnPayConfig paymentConfig;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired ProductSkuRepository skus;
    @Autowired CategoryRepository categories;
    @Autowired UserAddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @MockBean RefreshTokenService refresh;
    @MockBean StringRedisTemplate redis;
    @MockBean RedisCartService cart;
    @MockBean CacheManager caches;
    User seller, other, buyer;
    Shop shop;
    Long skuId, otherSkuId, productId, addressId;

    @BeforeEach void fixtures() {
        when(caches.getCache("products")).thenReturn(new ConcurrentMapCache("products"));
        seller = user(UserRole.ROLE_SELLER); other = user(UserRole.ROLE_SELLER); buyer = user(UserRole.ROLE_CUSTOMER);
        shop = shops.saveAndFlush(Shop.builder().owner(seller).shopName("Shop-"+UUID.randomUUID()).status(ShopStatus.ACTIVE).build());
        Shop second = shops.saveAndFlush(Shop.builder().owner(other).shopName("Other-"+UUID.randomUUID()).status(ShopStatus.ACTIVE).build());
        Category category = categories.saveAndFlush(Category.builder().name("Test category").slug(UUID.randomUUID().toString()).build());
        Product product = product(shop,category); productId = product.getId(); skuId = sku(product).getId();
        otherSkuId = sku(product(second,category)).getId();
        addressId = addresses.saveAndFlush(UserAddress.builder().user(buyer).recipientName("Recipient").phone("0901234567").provinceCity("City")
                .district("District").ward("Ward").streetAddress("1 Test street").isDefault(true).build()).getId();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    User user(UserRole role) { return users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.test").fullName("Test buyer/seller").passwordHash("hash").role(role).status(UserStatus.ACTIVE).build()); }
    Product product(Shop owner, Category category) { return products.saveAndFlush(Product.builder().shop(owner).category(category).name("Product snapshot").slug(UUID.randomUUID().toString()).status(ProductStatus.ACTIVE).price(BigDecimal.valueOf(100000)).stockQuantity(20).build()); }
    ProductSku sku(Product product) { return skus.saveAndFlush(ProductSku.builder().product(product).skuCode(UUID.randomUUID().toString()).price(BigDecimal.valueOf(100000)).stockQuantity(20).skuAttributes("{\"Color\":\"Red\"}").build()); }
    String token(User user) { return "Bearer "+jwt.generateAccessToken(user.getEmail(),user.getId(),user.getRole().name(),user.getFullName()); }
    OrderResponse order(Long sku, PaymentMethod method) {
        var request = CheckoutBatchRequest.builder().addressId(addressId).shippingMethod("standard").paymentMethod(method).note("Call before delivery")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(sku).quantity(2).build())).build();
        return checkout.checkout(buyer.getId(),request,"fulfillment-"+UUID.randomUUID()).getFirst();
    }
    OrderResponse order() { return order(skuId,PaymentMethod.COD); }
    String body(OrderStatus expected, OrderStatus status) throws Exception { return json.writeValueAsString(new SellerOrderStatusRequest(expected,status)); }
    void transition(Long id, OrderStatus expected, OrderStatus status, int code) throws Exception {
        mvc.perform(patch("/api/v1/seller/orders/"+id+"/status").header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(body(expected,status)))
                .andExpect(status().is(code));
    }
    void paymentState(Long id, PaymentStatus status, LocalDateTime expires) {
        jdbc.update("update orders set payment_status=?, payment_expires_at=? where id=?",status.name(),expires,id);
    }
    SellerOrderResponse asSeller(Long id, OrderStatus expected, OrderStatus target) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(seller.getId(),seller.getEmail()),null,List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
        try { return service.update(seller.getId(),id,new SellerOrderStatusRequest(expected,target)); }
        finally { SecurityContextHolder.clearContext(); }
    }

    @Test void listIsShopScopedPaginatedSearchableAndFilteredWithoutItemCollections() throws Exception {
        var first = order(); var confirmed = order(); order(otherSkuId,PaymentMethod.COD);
        asSeller(confirmed.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED);
        mvc.perform(get("/api/v1/seller/orders").param("shopId","999").param("customerId","999").param("page","2").param("size","1").header("Authorization",token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2)).andExpect(jsonPath("$.data.items[0].shopId").value(shop.getId()))
                .andExpect(jsonPath("$.data.items[0].items.length()").value(0));
        mvc.perform(get("/api/v1/seller/orders").param("keyword",first.getOrderCode().toLowerCase(Locale.ROOT)).param("status","PENDING").param("paymentStatus","UNPAID").header("Authorization",token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.items[0].id").value(first.getId()));
        for(String query : List.of("?page=0","?size=51","?status=UNKNOWN","?paymentStatus=UNKNOWN","?keyword="+"x".repeat(65)))
            mvc.perform(get("/api/v1/seller/orders"+query).header("Authorization",token(seller))).andExpect(status().isBadRequest());
    }

    @Test void detailContainsSnapshotsRecipientAndAuthoritativeActions() throws Exception {
        var order = order();
        mvc.perform(get("/api/v1/seller/orders/"+order.getId()).header("Authorization",token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.order.shippingAddressSnapshot").value(org.hamcrest.Matchers.containsString("0901234567")))
                .andExpect(jsonPath("$.data.order.note").value("Call before delivery")).andExpect(jsonPath("$.data.order.items[0].productNameSnapshot").value("Product snapshot"))
                .andExpect(jsonPath("$.data.order.items[0].priceAtPurchase").value(100000)).andExpect(jsonPath("$.data.order.finalAmount").value(230000))
                .andExpect(jsonPath("$.data.allowedStatuses").value(org.hamcrest.Matchers.containsInAnyOrder("CONFIRMED","CANCELLED")));
    }

    @Test void sequentialFulfillmentDoesNotChangeStockOrInventCodPayment() throws Exception {
        var order = order();
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,200);
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.PROCESSING,200);
        transition(order.getId(),OrderStatus.PROCESSING,OrderStatus.SHIPPING,200);
        transition(order.getId(),OrderStatus.SHIPPING,OrderStatus.CANCELLED,400);
        transition(order.getId(),OrderStatus.SHIPPING,OrderStatus.DELIVERED,200);
        transition(order.getId(),OrderStatus.DELIVERED,OrderStatus.RETURNED,400);
        mvc.perform(get("/api/v1/seller/orders/"+order.getId()).header("Authorization",token(seller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.order.orderStatus").value("DELIVERED")).andExpect(jsonPath("$.data.order.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.allowedStatuses.length()").value(0)).andExpect(jsonPath("$.data.actionNote").value(org.hamcrest.Matchers.containsString("COD chưa được đối soát")));
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(18);
        mvc.perform(get("/api/v1/orders/"+order.getId()).header("Authorization",token(buyer))).andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("DELIVERED"));
    }

    @Test void rejectsAnonymousWrongRolesStaleClaimsAndInactiveSellerOrShop() throws Exception {
        var order = order();
        mvc.perform(get("/api/v1/seller/orders")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/seller/orders/"+order.getId()+"/status").contentType(MediaType.APPLICATION_JSON).content(body(OrderStatus.PENDING,OrderStatus.CONFIRMED))).andExpect(status().isUnauthorized());
        for(User user : List.of(buyer,user(UserRole.ROLE_ADMIN))) {
            mvc.perform(get("/api/v1/seller/orders").header("Authorization",token(user))).andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/seller/orders/"+order.getId()+"/status").header("Authorization",token(user)).contentType(MediaType.APPLICATION_JSON).content(body(OrderStatus.PENDING,OrderStatus.CONFIRMED))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/seller/orders").header("Authorization",token(user(UserRole.ROLE_SELLER)))).andExpect(status().isNotFound());
        String oldToken = token(seller);
        seller.setRole(UserRole.ROLE_CUSTOMER); users.saveAndFlush(seller);
        mvc.perform(get("/api/v1/seller/orders").header("Authorization",oldToken)).andExpect(status().isForbidden());
        seller.setRole(UserRole.ROLE_SELLER); seller.setStatus(UserStatus.BLOCKED); users.saveAndFlush(seller);
        mvc.perform(get("/api/v1/seller/orders/"+order.getId()).header("Authorization",oldToken)).andExpect(status().isForbidden());
        seller.setStatus(UserStatus.ACTIVE); users.saveAndFlush(seller); shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        mvc.perform(patch("/api/v1/seller/orders/"+order.getId()+"/status").header("Authorization",oldToken).contentType(MediaType.APPLICATION_JSON).content(body(OrderStatus.PENDING,OrderStatus.CONFIRMED))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/orders/"+order.getId()+"/status").param("status","CONFIRMED").header("Authorization",oldToken)).andExpect(status().isForbidden());
    }

    @Test void cannotReadOrMutateOtherShopEvenWithForgedBodyAndQuery() throws Exception {
        var foreign = order(otherSkuId,PaymentMethod.COD);
        mvc.perform(get("/api/v1/seller/orders/"+foreign.getId()).header("Authorization",token(seller))).andExpect(status().isForbidden());
        transition(foreign.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,403);
        var own = order();
        String forged = "{\"expectedStatus\":\"PENDING\",\"status\":\"CONFIRMED\",\"paymentStatus\":\"PAID\",\"shopId\":999,\"customerId\":999}";
        mvc.perform(patch("/api/v1/seller/orders/"+own.getId()+"/status").header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(forged))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.order.shopId").value(shop.getId())).andExpect(jsonPath("$.data.order.paymentStatus").value("UNPAID"));
    }

    @Test void validatesBodyAndPreventsSkippingOrReversingStepsIncludingLegacyRoute() throws Exception {
        var order = order();
        for(String invalid : List.of("{}","{","{\"status\":\"CONFIRMED\"}","{\"expectedStatus\":\"PENDING\",\"status\":\"UNKNOWN\"}"))
            mvc.perform(patch("/api/v1/seller/orders/"+order.getId()+"/status").header("Authorization",token(seller)).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.SHIPPING,400);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,200);
        mvc.perform(patch("/api/v1/orders/"+order.getId()+"/status").param("status","SHIPPING").header("Authorization",token(seller))).andExpect(status().isBadRequest());
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.PENDING,400);
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.RETURNED,400);
        transition(99999999L,OrderStatus.PENDING,OrderStatus.CONFIRMED,404);
    }

    @Test void oldExpectedStatusReturnsConflictAndDuplicateCurrentStepIsIdempotent() throws Exception {
        var order = order();
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,200);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,200);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.PROCESSING,409);
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.PROCESSING,200);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,409);
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(18);
    }

    @Test void unpaidVnpayCannotPackAndPaidVnpayCanShipButCannotCancel() throws Exception {
        var order = order(skuId,PaymentMethod.VNPAY);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,200);
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.PROCESSING,400);
        mvc.perform(patch("/api/v1/orders/"+order.getId()+"/status").param("status","PROCESSING").header("Authorization",token(seller))).andExpect(status().isBadRequest());
        paymentState(order.getId(),PaymentStatus.PAID,null);
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.CANCELLED,400);
        mvc.perform(patch("/api/v1/orders/"+order.getId()+"/status").param("status","CANCELLED").header("Authorization",token(seller))).andExpect(status().isBadRequest());
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.PROCESSING,200);
        transition(order.getId(),OrderStatus.PROCESSING,OrderStatus.SHIPPING,200);
        transition(order.getId(),OrderStatus.SHIPPING,OrderStatus.DELIVERED,200);
        assertThat(orders.findById(order.getId()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test void livePaymentIntentBlocksCancelButExpiredIntentCanCancel() throws Exception {
        var order = order(skuId,PaymentMethod.VNPAY);
        paymentState(order.getId(),PaymentStatus.PENDING,LocalDateTime.now().plusMinutes(10));
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED,400);
        assertThat(service.detail(seller.getId(),order.getId()).allowedStatuses()).doesNotContain(OrderStatus.CANCELLED);
        paymentState(order.getId(),PaymentStatus.PENDING,LocalDateTime.now().minusMinutes(1));
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED,200);
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(20);
    }

    @Test void cancelRestoresStockAndEvictsCacheExactlyOnceAndTerminalOrderCannotReopen() throws Exception {
        var order = order();
        assertThat(catalog.getById(productId).getStockQuantity()).isEqualTo(18);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,200);
        transition(order.getId(),OrderStatus.CONFIRMED,OrderStatus.PROCESSING,200);
        mvc.perform(patch("/api/v1/orders/"+order.getId()+"/status").param("status","CANCELLED").header("Authorization",token(buyer))).andExpect(status().isForbidden());
        transition(order.getId(),OrderStatus.PROCESSING,OrderStatus.CANCELLED,200);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED,200);
        assertThat(catalog.getById(productId).getStockQuantity()).isEqualTo(20);
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(20);
        transition(order.getId(),OrderStatus.CANCELLED,OrderStatus.CONFIRMED,400);
    }

    @Test void twoTabsConfirmAndCancelHaveOneWinnerWithoutDoubleInventoryMutation() throws Exception {
        var order = order();
        assertThat(race(() -> {asSeller(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED);return 200;}, () -> {asSeller(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED);return 200;}))
                .containsExactlyInAnyOrder(200,409);
        var stored = orders.findById(order.getId()).orElseThrow();
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(stored.getOrderStatus() == OrderStatus.CANCELLED ? 20 : 18);
    }

    @Test void concurrentCancelRequestsAreIdempotent() throws Exception {
        var order = order();
        assertThat(race(() -> {asSeller(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED);return 200;}, () -> {asSeller(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED);return 200;})).containsExactly(200,200);
        assertThat(products.findById(productId).orElseThrow().getStockQuantity()).isEqualTo(20);
    }

    @Test void refundedOrderHasNoFulfillmentOrCancellationActions() throws Exception {
        var order = order(skuId,PaymentMethod.VNPAY);
        paymentState(order.getId(),PaymentStatus.REFUNDED,null);
        assertThat(service.detail(seller.getId(),order.getId()).allowedStatuses()).isEmpty();
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED,400);
        transition(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED,400);
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(18);
    }

    Map<String,String> signedCallback(OrderResponse order) {
        Map<String,String> fields = new HashMap<>();
        fields.put("vnp_TmnCode",paymentConfig.getTmnCode()); fields.put("vnp_TxnRef",order.getOrderCode());
        fields.put("vnp_Amount",order.getFinalAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        fields.put("vnp_ResponseCode","00"); fields.put("vnp_TransactionStatus","00"); fields.put("vnp_TransactionNo",Long.toString(ThreadLocalRandom.current().nextLong(1,Long.MAX_VALUE)));
        fields.put("vnp_SecureHash",VnPayUtils.hashAllFields(fields,paymentConfig.getHashSecret()));
        return fields;
    }

    @Test void paymentConfirmationPreservesSellerStageAndEnablesPacking() {
        var order = order(skuId,PaymentMethod.VNPAY);
        asSeller(order.getId(),OrderStatus.PENDING,OrderStatus.CONFIRMED);
        assertThat(payment.processVnPayIpn(signedCallback(order)).getRspCode()).isEqualTo("00");
        var detail = service.detail(seller.getId(),order.getId());
        assertThat(detail.order().getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(detail.order().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(detail.allowedStatuses()).containsExactly(OrderStatus.PROCESSING);
    }

    @Test void lateIpnForCancelledOrderIsRecordedWithoutReopeningOrRestockingTwice() throws Exception {
        var order = order(skuId,PaymentMethod.VNPAY);
        var fields = signedCallback(order);
        var outcomes = race(() -> {asSeller(order.getId(),OrderStatus.PENDING,OrderStatus.CANCELLED);return 200;}, () -> {assertThat(payment.processVnPayIpn(fields).getRspCode()).isEqualTo("00");return 200;});
        assertThat(outcomes).allMatch(code -> code == 200 || code == 400);
        var stored = orders.findById(order.getId()).orElseThrow();
        assertThat(stored.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(stored.getOrderStatus()).isIn(OrderStatus.CANCELLED,OrderStatus.PENDING);
        if(stored.getOrderStatus() == OrderStatus.CANCELLED) {
            var detail = service.detail(seller.getId(),order.getId());
            assertThat(detail.allowedStatuses()).isEmpty();
            assertThat(detail.actionNote()).contains("đối soát/hoàn tiền riêng");
        }
        assertThat(skus.findById(skuId).orElseThrow().getStockQuantity()).isEqualTo(stored.getOrderStatus() == OrderStatus.CANCELLED ? 20 : 18);
    }

    interface Action { int run(); }
    List<Integer> race(Action first, Action second) throws Exception {
        try(var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1), ready = new CountDownLatch(2);
            List<Future<Integer>> calls = new ArrayList<>();
            for(Action action : List.of(first,second)) calls.add(executor.submit(() -> {ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); try{return action.run();} catch(AppException error){return error.getErrorCode().getStatusCode().value();} finally{SecurityContextHolder.clearContext();}}));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(calls.get(0).get(20,TimeUnit.SECONDS),calls.get(1).get(20,TimeUnit.SECONDS));
        }
    }
}
