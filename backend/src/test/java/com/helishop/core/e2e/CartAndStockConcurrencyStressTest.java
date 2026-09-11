package com.helishop.core.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.constants.ProductStatus;
import com.helishop.core.common.constants.ShopStatus;
import com.helishop.core.common.exception.InsufficientStockException;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.cart.dto.AddToCartRequest;
import com.helishop.core.modules.cart.dto.CartResponse;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.CategoryRepository;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.ShopRepository;
import com.helishop.core.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@SpringBootTest
class CartAndStockConcurrencyStressTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private HashOperations<String, Object, Object> hashOperations;

    private User sampleCustomer;
    private Shop sampleShop;
    private Category sampleCategory;
    private Product sampleProduct;
    private ProductSku limitedSku;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RedisCartService redisCartService;
    private final Map<String, Map<Object, Object>> mockRedisStorage = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        shopRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tạo Customer
        sampleCustomer = userRepository.save(User.builder()
                .fullName("Khách hàng Concurrency")
                .email("stress.test@helishop.com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456")
                .role(com.helishop.core.common.constants.UserRole.ROLE_CUSTOMER)
                .status(com.helishop.core.common.constants.UserStatus.ACTIVE)
                .build());

        // 2. Tạo Shop
        sampleShop = shopRepository.save(Shop.builder()
                .shopName("HeliShop Official Mall")
                .owner(sampleCustomer)
                .status(ShopStatus.ACTIVE)
                .build());

        // 3. Tạo Category
        sampleCategory = categoryRepository.save(Category.builder()
                .name("Thiết bị thông minh")
                .slug("thiet-bi-thong-minh")
                .build());

        // 4. Tạo Product Flash Sale
        sampleProduct = productRepository.save(Product.builder()
                .name("Tai nghe HeliPods Pro Flash Sale")
                .slug("helipods-pro-flash-sale")
                .shop(sampleShop)
                .category(sampleCategory)
                .status(ProductStatus.ACTIVE)
                .stockQuantity(10) // Tổng 10 sản phẩm
                .price(BigDecimal.valueOf(1990000))
                .build());

        // 5. Tạo SKU với số lượng giới hạn = 10 chiếc
        limitedSku = productSkuRepository.save(ProductSku.builder()
                .skuCode("HELIPODS-PRO-WHITE")
                .product(sampleProduct)
                .price(BigDecimal.valueOf(1990000))
                .stockQuantity(10) // Tồn kho chỉ có đúng 10 cái!
                .skuAttributes("{\"color\": \"Trắng\", \"edition\": \"Pro\"}")
                .build());

        // 6. Cấu hình Mock Thread-Safe cho Redis Cart Hash
        mockRedisStorage.clear();
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object field = invocation.getArgument(1);
            Object value = invocation.getArgument(2);
            mockRedisStorage.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(field, value);
            return null;
        }).when(hashOperations).put(anyString(), any(), any());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object field = invocation.getArgument(1);
            Map<Object, Object> map = mockRedisStorage.get(key);
            return map != null ? map.get(field) : null;
        }).when(hashOperations).get(anyString(), any());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Map<Object, Object> map = mockRedisStorage.get(key);
            return map != null ? new HashMap<>(map) : Collections.emptyMap();
        }).when(hashOperations).entries(anyString());

        redisCartService = new RedisCartService(stringRedisTemplate, productSkuRepository, objectMapper);
    }

    @Test
    @DisplayName("Stress Test 1: Concurrency Cart - 200 requests đồng thời thao tác Redis Cart Hash mượt mà không xung đột")
    void shouldHandle200ConcurrentCartOperationsWithoutErrors() throws InterruptedException {
        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = (i % 20) + 1; // 20 users khác nhau đồng thời thao tác
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Đợi bắn đồng thời cùng 1 tích tắc
                    AddToCartRequest request = AddToCartRequest.builder()
                            .skuId(limitedSku.getId())
                            .quantity(1)
                            .build();

                    CartResponse response = redisCartService.addToCart(userId, request);
                    if (response != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Bắn đồng loạt 200 luồng
        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadCount);
        // Kiểm tra mock Redis storage đã chứa dữ liệu của các user
        assertThat(mockRedisStorage).isNotEmpty();
    }

    @Test
    @DisplayName("Stress Test 2: Concurrency Checkout (Chống âm kho) - 200 requests tranh mua SKU có tồn kho = 10 -> Đúng 10 thành công, tồn kho về đúng 0")
    void shouldEnsureStockNeverGoesBelowZeroUnder200ConcurrentCheckouts() throws InterruptedException {
        int totalRequests = 200;
        int initialStock = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger rejectedOrders = new AtomicInteger(0);
        List<String> createdOrderCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalRequests; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Cả 200 luồng cùng khởi chạy đồng thời

                    CheckoutRequest request = CheckoutRequest.builder()
                            .paymentMethod(PaymentMethod.COD)
                            .shippingAddressSnapshot("HeliShop Stress Test Street, Da Nang")
                            .items(List.of(CheckoutRequest.ItemRequest.builder()
                                    .skuId(limitedSku.getId())
                                    .quantity(1)
                                    .build()))
                            .build();

                    OrderResponse response = orderService.checkout(sampleCustomer.getId(), request);
                    if (response != null) {
                        successfulOrders.incrementAndGet();
                        createdOrderCodes.add(response.getOrderCode());
                    }
                } catch (InsufficientStockException e) {
                    // Ngoại lệ kỳ vọng khi tồn kho đã bị mua hết
                    rejectedOrders.incrementAndGet();
                } catch (Exception e) {
                    // Các ngoại lệ khác nếu có xung đột transaction
                    rejectedOrders.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Kích hoạt đồng thời 200 requests
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();

        // 1. Kiểm tra chính xác 10 đơn hàng thành công và 190 đơn bị từ chối
        assertThat(successfulOrders.get()).isEqualTo(initialStock);
        assertThat(rejectedOrders.get()).isEqualTo(totalRequests - initialStock);

        // 2. Kiểm tra tồn kho trong Database sau khi 200 luồng hoàn tất
        ProductSku finalSku = productSkuRepository.findById(limitedSku.getId()).orElseThrow();
        assertThat(finalSku.getStockQuantity())
                .as("Tồn kho SKU sau Flash Sale đồng thời 200 luồng phải bằng chính xác 0 và TUYỆT ĐỐI KHÔNG BỊ ÂM!")
                .isEqualTo(0);

        // 3. Kiểm tra số lượng Order trong database khớp với số lượng đơn thành công
        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(initialStock);
    }
}
