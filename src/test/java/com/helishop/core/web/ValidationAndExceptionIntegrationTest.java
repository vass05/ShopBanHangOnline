package com.helishop.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.exception.BadRequestException;
import com.helishop.core.common.exception.InsufficientStockException;
import com.helishop.core.common.exception.ResourceNotFoundException;
import com.helishop.core.modules.order.controller.OrderController;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.controller.ProductController;
import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"CUSTOMER", "SELLER", "ADMIN"})
class ValidationAndExceptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("Validation @Valid: Gửi body rỗng/sai trả về HTTP 400 và map lỗi từng field")
    void shouldReturn400WithFieldErrorsMapWhenValidationFails() throws Exception {
        // Gửi ProductRequest rỗng, vi phạm @NotBlank name, @NotNull shopId, @NotNull categoryId
        ProductRequest invalidRequest = new ProductRequest();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1010))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"))
                .andExpect(jsonPath("$.errors.name").value("Tên sản phẩm không được để trống"))
                .andExpect(jsonPath("$.errors.shopId").value("Shop ID không được để trống"))
                .andExpect(jsonPath("$.errors.categoryId").value("Category ID không được để trống"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("ResourceNotFoundException: Trả về HTTP 404 và ApiResponse chuẩn")
    void shouldReturn404WhenResourceNotFoundExceptionThrown() throws Exception {
        when(productService.getById(999L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        mockMvc.perform(get("/api/v1/products/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1006))
                .andExpect(jsonPath("$.message", containsString("Product không tồn tại với id : '999'")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("InsufficientStockException: Trả về HTTP 409 Conflict khi sản phẩm hết hàng")
    void shouldReturn409ConflictWhenInsufficientStockExceptionThrown() throws Exception {
        when(orderService.checkout(anyLong(), any(CheckoutRequest.class)))
                .thenThrow(new InsufficientStockException("IP16PM-256", 5, 2));

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("123 Phố Huế, Hà Nội")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(1L).quantity(5).build()))
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1008))
                .andExpect(jsonPath("$.message", containsString("không đủ số lượng tồn kho (yêu cầu: 5, hiện có: 2)")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("BadRequestException: Trả về HTTP 400 Bad Request và ApiResponse chuẩn")
    void shouldReturn400WhenBadRequestExceptionThrown() throws Exception {
        when(productService.getById(888L))
                .thenThrow(new BadRequestException("Tham số truy vấn ID không hợp lệ"));

        mockMvc.perform(get("/api/v1/products/888")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011))
                .andExpect(jsonPath("$.message").value("Tham số truy vấn ID không hợp lệ"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
