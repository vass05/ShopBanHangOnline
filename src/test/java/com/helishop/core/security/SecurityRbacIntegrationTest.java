package com.helishop.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.service.ProductService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // Giữ nguyên SecurityFilterChain để kiểm tra phân quyền thực tế
class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Public Access: Người dùng chưa đăng nhập xem được chi tiết sản phẩm (HTTP 200)")
    void shouldAllowPublicAccessToGetProducts() throws Exception {
        when(productService.getById(1L))
                .thenReturn(ProductResponse.builder().id(1L).name("iPhone 16 Pro").build());

        mockMvc.perform(get("/api/v1/products/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("iPhone 16 Pro"));
    }

    @Test
    @DisplayName("401 Unauthorized: Tạo sản phẩm khi chưa đăng nhập (không truyền token)")
    void shouldReturn401WhenCreatingProductWithoutToken() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("MacBook Pro M4")
                .shopId(1L)
                .categoryId(2L)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @DisplayName("403 Forbidden: CUSTOMER không được phép tạo sản phẩm (chỉ SELLER / ADMIN được phép)")
    void shouldReturn403WhenCustomerAttemptsToCreateProduct() throws Exception {
        String customerToken = jwtUtils.generateAccessToken("customer@test.com", 1L, "ROLE_CUSTOMER", "Khách hàng");

        ProductRequest request = ProductRequest.builder()
                .name("MacBook Pro M4")
                .shopId(1L)
                .categoryId(2L)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1005))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập chức năng này"));
    }

    @Test
    @DisplayName("201 Created: SELLER được phép tạo mới sản phẩm thành công")
    void shouldAllowSellerToCreateProduct() throws Exception {
        String sellerToken = jwtUtils.generateAccessToken("seller@test.com", 2L, "ROLE_SELLER", "Chủ Shop");

        ProductRequest request = ProductRequest.builder()
                .name("MacBook Pro M4")
                .shopId(1L)
                .categoryId(2L)
                .build();

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(ProductResponse.builder().id(10L).name("MacBook Pro M4").build());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro M4"));
    }

    @Test
    @DisplayName("403 Forbidden: SELLER không được phép tạo đơn checkout (chỉ CUSTOMER được phép)")
    void shouldReturn403WhenSellerAttemptsToCheckout() throws Exception {
        String sellerToken = jwtUtils.generateAccessToken("seller@test.com", 2L, "ROLE_SELLER", "Chủ Shop");

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("123 Phố Huế, Hà Nội")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(1L).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    @DisplayName("201 Created: CUSTOMER được phép tạo đơn checkout thành công")
    void shouldAllowCustomerToCheckout() throws Exception {
        String customerToken = jwtUtils.generateAccessToken("customer@test.com", 1L, "ROLE_CUSTOMER", "Khách hàng");

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("123 Phố Huế, Hà Nội")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(1L).quantity(1).build()))
                .build();

        when(orderService.checkout(anyLong(), any(CheckoutRequest.class)))
                .thenReturn(OrderResponse.builder().id(100L).orderCode("ORD-2026-001").totalAmount(BigDecimal.valueOf(1500000)).build());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-2026-001"));
    }
}
