package com.helishop.core.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.ProductStatus;
import com.helishop.core.common.constants.ShopStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.cart.dto.AddToCartRequest;
import com.helishop.core.modules.cart.dto.CartItemDto;
import com.helishop.core.modules.cart.dto.CartResponse;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCartServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ProductSkuRepository productSkuRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisCartService redisCartService;

    private Shop shop1;
    private Shop shop2;
    private Product product1;
    private Product product2;
    private ProductSku sku1;
    private ProductSku sku2;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        redisCartService = new RedisCartService(stringRedisTemplate, productSkuRepository, objectMapper);

        shop1 = Shop.builder()
                .shopName("Anker Official Store")
                .status(ShopStatus.ACTIVE)
                .build();
        shop1.setId(3L);

        shop2 = Shop.builder()
                .shopName("Apple Flagship Store")
                .status(ShopStatus.ACTIVE)
                .build();
        shop2.setId(7L);

        Category category = Category.builder().name("Phụ kiện").slug("phu-kien").build();

        product1 = Product.builder()
                .name("Củ sạc nhanh Anker 20W GaN")
                .shop(shop1)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://res.cloudinary.com/anker-white.jpg")
                .build();
        product1.setId(42L);

        product2 = Product.builder()
                .name("Tai nghe AirPods Pro 2")
                .shop(shop2)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://res.cloudinary.com/airpods.jpg")
                .build();
        product2.setId(88L);

        sku1 = ProductSku.builder()
                .product(product1)
                .skuCode("ANKER-20W-W")
                .skuAttributes("Màu Trắng, Chân tròn")
                .skuImageUrl("https://res.cloudinary.com/anker-white.jpg")
                .price(BigDecimal.valueOf(250000))
                .stockQuantity(50)
                .build();
        sku1.setId(105L);

        sku2 = ProductSku.builder()
                .product(product2)
                .skuCode("AIRPODS-PRO-2")
                .skuAttributes("Bản Type-C")
                .skuImageUrl("https://res.cloudinary.com/airpods.jpg")
                .price(BigDecimal.valueOf(5000000))
                .stockQuantity(10)
                .build();
        sku2.setId(201L);
    }

    @Test
    @DisplayName("Thêm sản phẩm mới vào giỏ hàng thành công, lưu vào Redis Hash và đặt TTL 30 ngày")
    void testAddToCart_Success_NewItem() throws Exception {
        Long userId = 1L;
        AddToCartRequest request = AddToCartRequest.builder()
                .skuId(105L)
                .quantity(2)
                .build();

        when(productSkuRepository.findByIdWithProductAndShop(105L)).thenReturn(Optional.of(sku1));
        when(hashOperations.get("cart:user:1", "105")).thenReturn(null);

        // Mock kết quả entries sau khi thêm
        CartItemDto expectedItem = CartItemDto.builder()
                .skuId(105L)
                .productId(42L)
                .shopId(3L)
                .shopName("Anker Official Store")
                .productTitle("Củ sạc nhanh Anker 20W GaN")
                .skuVariant("Màu Trắng, Chân tròn")
                .imageUrl("https://res.cloudinary.com/anker-white.jpg")
                .price(BigDecimal.valueOf(250000))
                .quantity(2)
                .stockAvailable(50)
                .updatedAt(1773289200L)
                .build();

        Map<Object, Object> entries = new HashMap<>();
        entries.put("105", objectMapper.writeValueAsString(expectedItem));
        when(hashOperations.entries("cart:user:1")).thenReturn(entries);

        CartResponse response = redisCartService.addToCart(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getTotalQuantity()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(response.getShops()).hasSize(1);
        assertThat(response.getShops().get(0).getShopName()).isEqualTo("Anker Official Store");

        verify(hashOperations).put(eq("cart:user:1"), eq("105"), any(String.class));
        verify(stringRedisTemplate).expire("cart:user:1", Duration.ofSeconds(RedisCartService.CART_TTL_SECONDS));
    }

    @Test
    @DisplayName("Thêm số lượng vượt quá tồn kho khả dụng -> Báo lỗi INSUFFICIENT_STOCK")
    void testAddToCart_ExceedStock_ThrowsException() {
        Long userId = 1L;
        AddToCartRequest request = AddToCartRequest.builder()
                .skuId(105L)
                .quantity(51) // Tồn kho chỉ có 50
                .build();

        when(productSkuRepository.findByIdWithProductAndShop(105L)).thenReturn(Optional.of(sku1));
        when(hashOperations.get("cart:user:1", "105")).thenReturn(null);

        assertThatThrownBy(() -> redisCartService.addToCart(userId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("Cập nhật số lượng sản phẩm trong giỏ hàng thành công")
    void testUpdateItemQuantity_Success() throws Exception {
        Long userId = 1L;
        Long skuId = 105L;
        int newQty = 4;

        CartItemDto currentItem = CartItemDto.builder()
                .skuId(105L)
                .productId(42L)
                .shopId(3L)
                .shopName("Anker Official Store")
                .productTitle("Củ sạc nhanh Anker 20W GaN")
                .price(BigDecimal.valueOf(250000))
                .quantity(2)
                .stockAvailable(50)
                .build();

        when(hashOperations.get("cart:user:1", "105")).thenReturn(objectMapper.writeValueAsString(currentItem));
        when(productSkuRepository.findByIdWithProductAndShop(105L)).thenReturn(Optional.of(sku1));

        CartItemDto updatedItem = CartItemDto.builder()
                .skuId(105L)
                .productId(42L)
                .shopId(3L)
                .shopName("Anker Official Store")
                .productTitle("Củ sạc nhanh Anker 20W GaN")
                .price(BigDecimal.valueOf(250000))
                .quantity(newQty)
                .stockAvailable(50)
                .build();

        Map<Object, Object> entries = new HashMap<>();
        entries.put("105", objectMapper.writeValueAsString(updatedItem));
        when(hashOperations.entries("cart:user:1")).thenReturn(entries);

        CartResponse response = redisCartService.updateItemQuantity(userId, skuId, newQty);

        assertThat(response.getTotalQuantity()).isEqualTo(4);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        verify(stringRedisTemplate).expire("cart:user:1", Duration.ofSeconds(RedisCartService.CART_TTL_SECONDS));
    }

    @Test
    @DisplayName("Cập nhật số lượng về 0 -> Tự động xóa sản phẩm khỏi giỏ")
    void testUpdateItemQuantity_Zero_DeletesItem() {
        Long userId = 1L;
        Long skuId = 105L;

        when(hashOperations.size("cart:user:1")).thenReturn(0L);
        when(hashOperations.entries("cart:user:1")).thenReturn(Map.of());

        CartResponse response = redisCartService.updateItemQuantity(userId, skuId, 0);

        assertThat(response.getTotalItems()).isZero();
        verify(hashOperations).delete("cart:user:1", "105");
        verify(stringRedisTemplate).delete("cart:user:1");
    }

    @Test
    @DisplayName("Phân nhóm sản phẩm theo từng Shop chuẩn Shopee và tính tổng phụ chính xác")
    void testGetCartGroupedByShop_MultipleShops() throws Exception {
        Long userId = 1L;

        CartItemDto item1 = CartItemDto.builder()
                .skuId(105L)
                .productId(42L)
                .shopId(3L)
                .shopName("Anker Official Store")
                .productTitle("Củ sạc nhanh Anker 20W GaN")
                .price(BigDecimal.valueOf(250000))
                .quantity(2)
                .stockAvailable(50)
                .build();

        CartItemDto item2 = CartItemDto.builder()
                .skuId(201L)
                .productId(88L)
                .shopId(7L)
                .shopName("Apple Flagship Store")
                .productTitle("Tai nghe AirPods Pro 2")
                .price(BigDecimal.valueOf(5000000))
                .quantity(1)
                .stockAvailable(10)
                .build();

        Map<Object, Object> entries = new HashMap<>();
        entries.put("105", objectMapper.writeValueAsString(item1));
        entries.put("201", objectMapper.writeValueAsString(item2));
        when(hashOperations.entries("cart:user:1")).thenReturn(entries);

        CartResponse response = redisCartService.getCartGroupedByShop(userId);

        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getTotalQuantity()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5500000));
        assertThat(response.getShops()).hasSize(2);

        // Shop 1
        assertThat(response.getShops().get(0).getShopId()).isEqualTo(3L);
        assertThat(response.getShops().get(0).getShopName()).isEqualTo("Anker Official Store");
        assertThat(response.getShops().get(0).getShopTotal()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(response.getShops().get(0).getShopItemCount()).isEqualTo(2);

        // Shop 2
        assertThat(response.getShops().get(1).getShopId()).isEqualTo(7L);
        assertThat(response.getShops().get(1).getShopName()).isEqualTo("Apple Flagship Store");
        assertThat(response.getShops().get(1).getShopTotal()).isEqualByComparingTo(BigDecimal.valueOf(5000000));
        assertThat(response.getShops().get(1).getShopItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Xóa nhiều sản phẩm khỏi giỏ hàng")
    void testRemoveItemsFromCart() {
        Long userId = 1L;
        List<Long> skuIds = List.of(105L, 201L);

        when(hashOperations.size("cart:user:1")).thenReturn(0L);
        when(hashOperations.entries("cart:user:1")).thenReturn(Map.of());

        CartResponse response = redisCartService.removeItemsFromCart(userId, skuIds);

        assertThat(response.getTotalItems()).isZero();
        verify(hashOperations).delete("cart:user:1", "105", "201");
        verify(stringRedisTemplate).delete("cart:user:1");
    }

    @Test
    @DisplayName("Làm sạch toàn bộ giỏ hàng")
    void testClearCart() {
        Long userId = 1L;
        redisCartService.clearCart(userId);
        verify(stringRedisTemplate).delete("cart:user:1");
    }
}
