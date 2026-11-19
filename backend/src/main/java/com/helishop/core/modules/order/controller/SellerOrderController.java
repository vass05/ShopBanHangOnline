package com.helishop.core.modules.order.controller;

import com.helishop.core.common.constants.*;
import com.helishop.core.common.response.*;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.service.SellerOrderService;
import com.helishop.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/seller/orders") @RequiredArgsConstructor @PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller order fulfillment", description = "Xác nhận, đóng gói và giao đơn của shop đang đăng nhập")
public class SellerOrderController {
    private final SellerOrderService service;
    private final SecurityUtils security;

    @GetMapping
    @Operation(summary = "Danh sách đơn bán", description = "Phân trang 1-based, size tối đa 50, tìm mã đơn/lọc trạng thái và thanh toán. Shop lấy từ JWT; items chỉ có trong chi tiết.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách riêng shop"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bộ lọc không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không phải seller/shop đang hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Chưa có shop")})
    public ApiResponse<PageResponse<OrderResponse>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) OrderStatus status, @RequestParam(required = false) PaymentStatus paymentStatus) {
        return ApiResponse.success(service.list(security.getCurrentUserId(null), page, size, keyword, status, paymentStatus));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết đơn bán", description = "Trả snapshot hàng/giá/địa chỉ và danh sách trạng thái được phép chuyển theo dữ liệu thanh toán mới nhất")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đơn bán và thao tác hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sai chủ shop hoặc bị khóa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tồn tại")})
    public ApiResponse<SellerOrderResponse> detail(@PathVariable Long id) { return ApiResponse.success(service.detail(security.getCurrentUserId(null), id)); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Xử lý đơn bán", description = "Gửi expectedStatus và status; tuần tự xác nhận → đóng gói → bàn giao → đã giao, khóa order chống race. Không tự thu tiền COD/hoàn tiền. Bước hiện tại trùng target trả thành công không lặp tác dụng phụ.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trạng thái và thao tác tiếp theo mới nhất"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bước hoặc tình trạng thanh toán không cho phép"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sai chủ shop hoặc bị khóa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tồn tại"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Trạng thái đã thay đổi, cần tải lại")})
    public ApiResponse<SellerOrderResponse> update(@PathVariable Long id, @Valid @RequestBody SellerOrderStatusRequest request) {
        return ApiResponse.success(service.update(security.getCurrentUserId(null), id, request), "Đã cập nhật trạng thái đơn bán");
    }
}
