package com.helishop.core.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.common.exception.InsufficientStockException;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Sprint 6: Checkout API Flow Integration Tests")
class CheckoutFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(username = "customer@helishop.com", roles = "CUSTOMER")
    @DisplayName("Integration Test 1: Khách hàng (ROLE_CUSTOMER) checkout hợp lệ -> Trả về HTTP 201 Created và ApiResponse<OrderResponse>")
    void shouldCheckoutSuccessfully_WhenCustomerRoleAndValidPayload() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội")
                .note("Giao giờ hành chính")
                .items(List.of(
                        CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(2).build()
                ))
                .build();

        OrderResponse mockOrderResponse = OrderResponse.builder()
                .id(500L)
                .orderCode("ORD-A1B2C3D4")
                .customerId(1L)
                .customerName("Khách hàng Test")
                .totalAmount(BigDecimal.valueOf(40000000))
                .finalAmount(BigDecimal.valueOf(40000000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội")
                .build();

        when(orderService.checkout(eq(1L), any(CheckoutRequest.class))).thenReturn(mockOrderResponse);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Đặt hàng thành công"))
                .andExpect(jsonPath("$.data.id").value(500))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-A1B2C3D4"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.totalAmount").value(40000000));
    }

    @Test
    @WithMockUser(username = "customer@helishop.com", roles = "CUSTOMER")
    @DisplayName("Integration Test 2: Kho không đủ tồn kho -> Trả về HTTP 409 Conflict với thông điệp InsufficientStockException")
    void shouldReturn409Conflict_WhenStockIsInsufficient() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Số 1 Đại Cồ Việt, Hà Nội")
                .items(List.of(
                        CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(10).build()
                ))
                .build();

        when(orderService.checkout(eq(1L), any(CheckoutRequest.class)))
                .thenThrow(new InsufficientStockException("IP16-FLASH-128", 10, 3));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1008))
                .andExpect(jsonPath("$.message").value(containsString("IP16-FLASH-128")))
                .andExpect(jsonPath("$.message").value(containsString("không đủ số lượng tồn kho")));
    }

    @Test
    @WithMockUser(username = "seller@helishop.com", roles = "SELLER")
    @DisplayName("Integration Test 3: Người dùng không có ROLE_CUSTOMER (ví dụ ROLE_SELLER) checkout -> Trả về HTTP 403 Forbidden")
    void shouldReturn403Forbidden_WhenUserIsNotCustomer() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Hà Nội")
                .items(List.of(
                        CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(1).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration Test 4: Người dùng chưa xác thực (Unauthenticated) gọi checkout -> Trả về HTTP 401 Unauthorized")
    void shouldReturn401Unauthorized_WhenUnauthenticated() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Hà Nội")
                .items(List.of(
                        CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(1).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@helishop.com", roles = "CUSTOMER")
    @DisplayName("Integration Test 5: Body payload không hợp lệ (@Valid fail) -> Trả về HTTP 400 Bad Request kèm chi tiết validation errors")
    void shouldReturn400BadRequest_WhenValidationFails() throws Exception {
        // Vi phạm: thiếu shippingAddressSnapshot và items rỗng
        CheckoutRequest invalidRequest = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot(null) // @NotNull
                .items(List.of()) // @NotEmpty
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1010))
                .andExpect(jsonPath("$.errors.shippingAddressSnapshot").exists())
                .andExpect(jsonPath("$.errors.items").exists());
    }
}
