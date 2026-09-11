package com.helishop.core.modules.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.cart.dto.AddToCartRequest;
import com.helishop.core.modules.cart.dto.CartItemDto;
import com.helishop.core.modules.cart.dto.CartResponse;
import com.helishop.core.modules.cart.dto.CartShopGroupResponse;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCartService {

    public static final String CART_KEY_PREFIX = "cart:user:";
    public static final long CART_TTL_SECONDS = 2592000L; // 30 ngày (30 * 24 * 3600)

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductSkuRepository productSkuRepository;
    private final ObjectMapper objectMapper;

    /**
     * Thêm sản phẩm vào giỏ hàng Redis Hash
     */
    @Transactional(readOnly = true)
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        validateUserId(userId);

        ProductSku sku = productSkuRepository.findByIdWithProductAndShop(request.getSkuId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy biến thể sản phẩm (SKU) với ID: " + request.getSkuId()));

        String cartKey = buildCartKey(userId);
        String field = String.valueOf(request.getSkuId());

        // Đọc món hàng hiện tại nếu đã có trong giỏ
        CartItemDto existingItem = getCartItem(cartKey, field);
        int currentQty = (existingItem != null) ? existingItem.getQuantity() : 0;
        int newQty = currentQty + request.getQuantity();

        // Kiểm tra tồn kho khả dụng
        if (newQty > sku.getStockQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Sản phẩm '%s' chỉ còn %d món trong kho, không thể thêm %d món",
                            sku.getProduct().getName(), sku.getStockQuantity(), newQty));
        }

        String imageUrl = sku.getSkuImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = sku.getProduct().getMainImageUrl();
        }

        String variant = sku.getSkuAttributes();
        if (variant == null || variant.isBlank()) {
            variant = sku.getSkuCode();
        }

        CartItemDto itemDto = CartItemDto.builder()
                .skuId(sku.getId())
                .productId(sku.getProduct().getId())
                .shopId(sku.getProduct().getShop().getId())
                .shopName(sku.getProduct().getShop().getShopName())
                .productTitle(sku.getProduct().getName())
                .skuVariant(variant)
                .imageUrl(imageUrl)
                .price(sku.getPrice())
                .quantity(newQty)
                .stockAvailable(sku.getStockQuantity())
                .updatedAt(Instant.now().getEpochSecond())
                .build();

        saveCartItem(cartKey, field, itemDto);
        refreshCartTtl(cartKey);

        return getCartGroupedByShop(userId);
    }

    /**
     * Cập nhật số lượng của một sản phẩm trong giỏ hàng
     */
    @Transactional(readOnly = true)
    public CartResponse updateItemQuantity(Long userId, Long skuId, Integer quantity) {
        validateUserId(userId);
        String cartKey = buildCartKey(userId);
        String field = String.valueOf(skuId);

        if (quantity == null || quantity <= 0) {
            stringRedisTemplate.opsForHash().delete(cartKey, field);
            cleanupIfEmpty(cartKey);
            return getCartGroupedByShop(userId);
        }

        CartItemDto existingItem = getCartItem(cartKey, field);
        if (existingItem == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Sản phẩm không tồn tại trong giỏ hàng");
        }

        ProductSku sku = productSkuRepository.findByIdWithProductAndShop(skuId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy biến thể sản phẩm với ID: " + skuId));

        if (quantity > sku.getStockQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Sản phẩm chỉ còn %d món trong kho", sku.getStockQuantity()));
        }

        existingItem.setQuantity(quantity);
        existingItem.setPrice(sku.getPrice());
        existingItem.setStockAvailable(sku.getStockQuantity());
        existingItem.setUpdatedAt(Instant.now().getEpochSecond());

        saveCartItem(cartKey, field, existingItem);
        refreshCartTtl(cartKey);

        return getCartGroupedByShop(userId);
    }

    /**
     * Lấy toàn bộ giỏ hàng của người dùng, tự động phân nhóm theo Shop và tính tổng tiền
     */
    public CartResponse getCartGroupedByShop(Long userId) {
        validateUserId(userId);
        String cartKey = buildCartKey(userId);

        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey);
        if (entries == null || entries.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
                    .shops(new ArrayList<>())
                    .totalItems(0)
                    .totalQuantity(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        // Parse danh sách items
        List<CartItemDto> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String json = (String) entry.getValue();
            try {
                CartItemDto item = objectMapper.readValue(json, CartItemDto.class);
                items.add(item);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse JSON cho cart item [userId={}, skuId={}]: {}", userId, entry.getKey(), e.getMessage());
            }
        }

        // Nhóm các items theo shopId (sắp xếp tăng dần theo shopId để đảm bảo tính xác định)
        Map<Long, List<CartItemDto>> shopGroups = new java.util.TreeMap<>();
        for (CartItemDto item : items) {
            shopGroups.computeIfAbsent(item.getShopId(), k -> new ArrayList<>()).add(item);
        }

        List<CartShopGroupResponse> shopResponses = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, List<CartItemDto>> entry : shopGroups.entrySet()) {
            List<CartItemDto> shopItems = entry.getValue();
            String shopName = shopItems.isEmpty() ? "Unknown Shop" : shopItems.get(0).getShopName();

            BigDecimal shopTotal = shopItems.stream()
                    .map(CartItemDto::getSubTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int shopItemCount = shopItems.stream()
                    .mapToInt(CartItemDto::getQuantity)
                    .sum();

            shopResponses.add(CartShopGroupResponse.builder()
                    .shopId(entry.getKey())
                    .shopName(shopName)
                    .items(shopItems)
                    .shopTotal(shopTotal)
                    .shopItemCount(shopItemCount)
                    .build());

            totalQuantity += shopItemCount;
            totalAmount = totalAmount.add(shopTotal);
        }

        return CartResponse.builder()
                .userId(userId)
                .shops(shopResponses)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * Xóa danh sách sản phẩm đã chọn khỏi giỏ hàng
     */
    public CartResponse removeItemsFromCart(Long userId, List<Long> skuIds) {
        validateUserId(userId);
        if (skuIds == null || skuIds.isEmpty()) {
            return getCartGroupedByShop(userId);
        }

        String cartKey = buildCartKey(userId);
        Object[] fields = skuIds.stream().map(String::valueOf).toArray(Object[]::new);
        stringRedisTemplate.opsForHash().delete(cartKey, fields);

        cleanupIfEmpty(cartKey);

        return getCartGroupedByShop(userId);
    }

    /**
     * Xóa sạch toàn bộ giỏ hàng
     */
    public void clearCart(Long userId) {
        validateUserId(userId);
        String cartKey = buildCartKey(userId);
        stringRedisTemplate.delete(cartKey);
    }

    // --- Helper Methods ---

    private String buildCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Mã người dùng không hợp lệ");
        }
    }

    private CartItemDto getCartItem(String cartKey, String field) {
        Object val = stringRedisTemplate.opsForHash().get(cartKey, field);
        if (val == null) return null;
        try {
            return objectMapper.readValue((String) val, CartItemDto.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse CartItemDto từ Redis hash [key={}, field={}]: {}", cartKey, field, e.getMessage());
            return null;
        }
    }

    private void saveCartItem(String cartKey, String field, CartItemDto item) {
        try {
            String json = objectMapper.writeValueAsString(item);
            stringRedisTemplate.opsForHash().put(cartKey, field, json);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize CartItemDto sang JSON: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi lưu trữ giỏ hàng Redis");
        }
    }

    private void refreshCartTtl(String cartKey) {
        stringRedisTemplate.expire(cartKey, Duration.ofSeconds(CART_TTL_SECONDS));
    }

    private void cleanupIfEmpty(String cartKey) {
        Long size = stringRedisTemplate.opsForHash().size(cartKey);
        if (size == null || size == 0) {
            stringRedisTemplate.delete(cartKey);
        } else {
            refreshCartTtl(cartKey);
        }
    }
}
