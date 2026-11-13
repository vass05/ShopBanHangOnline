package com.helishop.core.order;

import com.helishop.core.common.constants.*;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.order.service.CheckoutService;
import com.helishop.core.modules.product.entity.*;
import com.helishop.core.modules.product.repository.*;
import com.helishop.core.modules.user.entity.*;
import com.helishop.core.modules.user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:checkout_batch_tests;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
class CheckoutDatabaseIntegrationTest {
    @Autowired CheckoutService service;
    @Autowired UserRepository users;
    @Autowired UserAddressRepository addresses;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductSkuRepository skus;
    @Autowired OrderRepository orders;
    @Autowired com.helishop.core.modules.order.service.OrderLifecycleService lifecycle;
    @MockBean RedisCartService cart;
    @Autowired com.helishop.core.modules.payment.service.PaymentService payment;
    @Autowired com.helishop.core.modules.payment.config.VnPayConfig vnPayConfig;
    @Autowired com.helishop.core.modules.payment.repository.PaymentTransactionRepository transactions;
    @Autowired com.helishop.core.modules.payment.event.OrderPaidOutboxRepository outbox;
    Long customerId;
    Long addressId;
    List<Long> skuIds;

    @BeforeEach void fixtures() {
        String suffix = UUID.randomUUID().toString();
        var customer = users.save(User.builder().email(suffix + "@test.com").fullName("Buyer").passwordHash("hash")
                .role(UserRole.ROLE_CUSTOMER).status(UserStatus.ACTIVE).build());
        customerId = customer.getId();
        addressId = addresses.save(UserAddress.builder().user(customer).recipientName("Buyer").phone("0900000000")
                .streetAddress("Street").ward("Ward").district("District").provinceCity("City").isDefault(true).build()).getId();
        var category = categories.save(Category.builder().name("Category").slug(suffix).build());
        skuIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            var seller = users.save(User.builder().email(i + suffix + "@seller.com").fullName("Seller").passwordHash("hash")
                    .role(UserRole.ROLE_SELLER).status(UserStatus.ACTIVE).build());
            var shop = shops.save(Shop.builder().owner(seller).shopName(i + suffix).status(ShopStatus.ACTIVE).build());
            var product = products.save(Product.builder().shop(shop).category(category).name("Product " + i).slug(i + suffix)
                    .price(BigDecimal.valueOf(100000)).stockQuantity(10).status(ProductStatus.ACTIVE).build());
            skuIds.add(skus.save(ProductSku.builder().product(product).skuCode(i + suffix).price(BigDecimal.valueOf(100000))
                    .stockQuantity(10).skuAttributes("{}").build()).getId());
        }
    }

    CheckoutBatchRequest request() {
        return CheckoutBatchRequest.builder().addressId(addressId).shippingMethod("standard").paymentMethod(PaymentMethod.COD)
                .items(skuIds.stream().map(id -> CheckoutRequest.ItemRequest.builder().skuId(id).quantity(1).build()).toList()).build();
    }

    @Test void persistsSeparateShopOrdersAndDeductsStockInRealTransactions() {
        var result = service.checkout(customerId, request(), "database-checkout-0001");
        assertEquals(2, result.size());
        assertEquals(2, result.stream().map(OrderResponse::getShopId).distinct().count());
        assertEquals(0, result.stream().map(OrderResponse::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.valueOf(260000)));
        for (Long skuId : skuIds) assertEquals(9, skus.findById(skuId).orElseThrow().getStockQuantity());
        verify(cart).consumePurchasedItems(eq(customerId), anyMap());
    }

    @Test void concurrentRepeatedRequestsReturnSameOrdersAndDeductOnce() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<List<OrderResponse>>> calls = new ArrayList<>();
            for (int i = 0; i < 4; i++) calls.add(executor.submit(() -> { start.await(); return service.checkout(customerId, request(), "concurrent-checkout-0001"); }));
            start.countDown();
            List<Long> first = calls.getFirst().get(10, TimeUnit.SECONDS).stream().map(OrderResponse::getId).toList();
            for (var call : calls) assertEquals(first, call.get(10, TimeUnit.SECONDS).stream().map(OrderResponse::getId).toList());
        }
        assertEquals(2, orders.findByCustomerId(customerId).size());
        for (Long skuId : skuIds) assertEquals(9, skus.findById(skuId).orElseThrow().getStockQuantity());
        verify(cart, times(1)).consumePurchasedItems(eq(customerId), anyMap());
    }

    @Test void databaseFailureRollsBackOrdersAndStockAndDoesNotConsumeCart() {
        var request = request(); request.setNote("x".repeat(501));
        assertThrows(RuntimeException.class, () -> service.checkout(customerId, request, "rollback-checkout-0001"));
        assertEquals(0, orders.findByCustomerId(customerId).size());
        for (Long skuId : skuIds) assertEquals(10, skus.findById(skuId).orElseThrow().getStockQuantity());
        verifyNoInteractions(cart);
    }

    @Test void simultaneousCancellationsRestoreStockOnceAndCannotReactivateOrder() throws Exception {
        var created = service.checkout(customerId, request(), "cancellation-checkout-0001");
        Long orderId = created.getFirst().getId();
        Long skuId = created.getFirst().getItems().getFirst().getSkuId();
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<OrderResponse>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) calls.add(executor.submit(() -> {
                authenticateAdmin();
                try { return lifecycle.update(orderId, OrderStatus.CANCELLED); }
                finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
            }));
            for (var call : calls) assertEquals(OrderStatus.CANCELLED, call.get(10, TimeUnit.SECONDS).getOrderStatus());
        }
        assertEquals(10, skus.findById(skuId).orElseThrow().getStockQuantity());
        authenticateAdmin();
        try { assertThrows(com.helishop.core.common.exception.AppException.class, () -> lifecycle.update(orderId, OrderStatus.CONFIRMED)); }
        finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        assertEquals(10, skus.findById(skuId).orElseThrow().getStockQuantity());
    }

    private void authenticateAdmin() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test void customerCannotAdvanceDeliveryOrCancelPaidOrder() {
        var created = service.checkout(customerId, request(), "customer-policy-checkout-0001");
        Long id = created.getFirst().getId();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.helishop.core.security.AuthenticatedUser(customerId, "buyer@test.com"), null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        try {
            assertThrows(com.helishop.core.common.exception.AppException.class, () -> lifecycle.update(id, OrderStatus.DELIVERED));
            var order = orders.findById(id).orElseThrow();
            order.setPaymentStatus(PaymentStatus.PAID); orders.save(order);
            assertThrows(com.helishop.core.common.exception.AppException.class, () -> lifecycle.update(id, OrderStatus.CANCELLED));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        assertEquals(OrderStatus.PENDING, orders.findById(id).orElseThrow().getOrderStatus());
    }

    private Map<String, String> signedCallback(OrderResponse order, boolean success) {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TmnCode", vnPayConfig.getTmnCode()); fields.put("vnp_TxnRef", order.getOrderCode());
        fields.put("vnp_Amount", order.getFinalAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        fields.put("vnp_ResponseCode", success ? "00" : "24"); fields.put("vnp_TransactionStatus", success ? "00" : "02");
        fields.put("vnp_TransactionNo", success ? "9" + order.getId() : "0");
        fields.put("vnp_PayDate", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        fields.put("vnp_SecureHash", com.helishop.core.modules.payment.util.VnPayUtils.hashAllFields(fields, vnPayConfig.getHashSecret()));
        return fields;
    }

    @Test void concurrentSuccessfulIpnPersistsOneTransactionAndOneOutboxEvent() throws Exception {
        var request = request(); request.setPaymentMethod(PaymentMethod.VNPAY);
        var order = service.checkout(customerId, request, "payment-concurrent-0001").getFirst();
        var callback = signedCallback(order, true);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<String>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) calls.add(executor.submit(() -> { start.await(); return payment.processVnPayIpn(callback).getRspCode(); }));
            start.countDown();
            Set<String> responses = new HashSet<>();
            for (var call : calls) responses.add(call.get(10, TimeUnit.SECONDS));
            assertEquals(Set.of("00", "02"), responses);
        }
        assertEquals(1, transactions.findByOrderId(order.getId()).size());
        assertEquals(1, outbox.findByOrderId(order.getId()).size());
        assertEquals(PaymentStatus.PAID, orders.findById(order.getId()).orElseThrow().getPaymentStatus());
    }

    @Test void repeatedFailedIpnIsIdempotentAndDoesNotQueueFulfilment() {
        var request = request(); request.setPaymentMethod(PaymentMethod.VNPAY);
        var order = service.checkout(customerId, request, "payment-failure-0001").getFirst();
        var callback = signedCallback(order, false);
        assertEquals("00", payment.processVnPayIpn(callback).getRspCode());
        assertEquals("02", payment.processVnPayIpn(callback).getRspCode());
        assertEquals(1, transactions.findByOrderId(order.getId()).size());
        assertEquals(0, outbox.findByOrderId(order.getId()).size());
    }

    @Test void browserReturnCannotMarkPaidBeforeTheIpnCommits() {
        var request = request(); request.setPaymentMethod(PaymentMethod.VNPAY);
        var order = service.checkout(customerId, request, "payment-return-0001").getFirst();
        var callback = signedCallback(order, true);
        assertEquals(PaymentStatus.PENDING, payment.processVnPayReturn(callback).getStatus());
        assertEquals(PaymentStatus.UNPAID, orders.findById(order.getId()).orElseThrow().getPaymentStatus());
        payment.processVnPayIpn(callback);
        assertEquals(PaymentStatus.PAID, payment.processVnPayReturn(callback).getStatus());
    }

    @Test void unpaidVnpayOrderCannotBeAdvancedToShipping() {
        var request = request(); request.setPaymentMethod(PaymentMethod.VNPAY);
        var order = service.checkout(customerId, request, "payment-shipping-0001").getFirst();
        authenticateAdmin();
        try {
            lifecycle.update(order.getId(), OrderStatus.CONFIRMED);
            assertThrows(com.helishop.core.common.exception.AppException.class, () -> lifecycle.update(order.getId(), OrderStatus.SHIPPING));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
}
