package com.helishop.core.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.cart.dto.AddToCartRequest;
import com.helishop.core.modules.cart.dto.CartItemDto;
import com.helishop.core.modules.cart.dto.CartResponse;
import com.helishop.core.modules.cart.dto.CartShopGroupResponse;
import com.helishop.core.modules.cart.dto.UpdateCartItemRequest;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private RedisCartService cartService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private String customerToken;
    private CartResponse sampleCartResponse;

    @BeforeEach
    void setUp() {
        customerToken = "Bearer " + jwtUtils.generateAccessToken("customer@gmail.com", 1L, "ROLE_CUSTOMER", "Nguyen Van A");

        CartItemDto item = CartItemDto.builder()
                .skuId(105L)
                .productId(42L)
                .shopId(3L)
                .shopName("Anker Official Store")
                .productTitle("Củ sạc nhanh Anker 20W GaN")
                .price(BigDecimal.valueOf(250000))
                .quantity(2)
                .stockAvailable(50)
                .build();

        CartShopGroupResponse shopGroup = CartShopGroupResponse.builder()
                .shopId(3L)
                .shopName("Anker Official Store")
                .items(List.of(item))
                .shopTotal(BigDecimal.valueOf(500000))
                .shopItemCount(2)
                .build();

        sampleCartResponse = CartResponse.builder()
                .userId(1L)
                .shops(List.of(shopGroup))
                .totalItems(1)
                .totalQuantity(2)
                .totalAmount(BigDecimal.valueOf(500000))
                .build();
    }

    @Test
    @DisplayName("Chưa đăng nhập -> Gọi GET /api/v1/cart trả về 401 Unauthorized")
    void shouldReturn401WhenAnonymousAccessCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Khách hàng đã đăng nhập -> Gọi GET /api/v1/cart trả về 200 OK kèm giỏ hàng phân nhóm theo Shop")
    void shouldReturnCartGroupedByShopWhenAuthenticated() throws Exception {
        when(cartService.getCartGroupedByShop(anyLong())).thenReturn(sampleCartResponse);

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", customerToken)
                        .param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shops[0].shopName").value("Anker Official Store"))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(500000.0));
    }

    @Test
    @DisplayName("Thêm sản phẩm vào giỏ hàng -> Gọi POST /api/v1/cart/items trả về 201 Created")
    void shouldAddToCartSuccessfully() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .skuId(105L)
                .quantity(2)
                .build();

        when(cartService.addToCart(anyLong(), any(AddToCartRequest.class))).thenReturn(sampleCartResponse);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken)
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @DisplayName("Cập nhật số lượng sản phẩm -> Gọi PUT /api/v1/cart/items/{skuId} trả về 200 OK")
    void shouldUpdateCartItemQuantity() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();

        when(cartService.updateItemQuantity(anyLong(), eq(105L), eq(3))).thenReturn(sampleCartResponse);

        mockMvc.perform(put("/api/v1/cart/items/105")
                        .header("Authorization", customerToken)
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Xóa một sản phẩm khỏi giỏ hàng -> Gọi DELETE /api/v1/cart/items/{skuId} trả về 200 OK")
    void shouldRemoveCartItem() throws Exception {
        when(cartService.removeItemsFromCart(anyLong(), eq(List.of(105L)))).thenReturn(sampleCartResponse);

        mockMvc.perform(delete("/api/v1/cart/items/105")
                        .header("Authorization", customerToken)
                        .param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Làm sạch giỏ hàng -> Gọi DELETE /api/v1/cart trả về 200 OK")
    void shouldClearCart() throws Exception {
        doNothing().when(cartService).clearCart(anyLong());

        mockMvc.perform(delete("/api/v1/cart")
                        .header("Authorization", customerToken)
                        .param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
