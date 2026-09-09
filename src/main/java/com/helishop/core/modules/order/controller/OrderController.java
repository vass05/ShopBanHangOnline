package com.helishop.core.modules.order.controller;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs đặt hàng, xử lý thanh toán và quản lý đơn hàng")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tiến hành đặt hàng và khóa tồn kho chống âm kho (Pessimistic Locking - PESSIMISTIC_WRITE)")
    public ApiResponse<OrderResponse> checkout(@RequestParam Long customerId,
                                              @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success(orderService.checkout(customerId, request), "Đặt hàng thành công");
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết đơn hàng theo ID")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.success(orderService.getById(id));
    }

    @GetMapping("/code/{orderCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết đơn hàng theo mã Order Code")
    public ApiResponse<OrderResponse> getOrderByCode(@PathVariable String orderCode) {
        return ApiResponse.success(orderService.getByOrderCode(orderCode));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách đơn hàng theo khách hàng")
    public ApiResponse<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ApiResponse.success(orderService.getByCustomerId(customerId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật trạng thái đơn hàng (Tự động hoàn kho khi CANCELLED hoặc RETURNED)")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                        @RequestParam OrderStatus status) {
        return ApiResponse.success(orderService.updateOrderStatus(id, status), "Cập nhật trạng thái đơn hàng thành công");
    }
}
