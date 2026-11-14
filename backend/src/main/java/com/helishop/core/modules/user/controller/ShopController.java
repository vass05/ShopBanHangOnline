package com.helishop.core.modules.user.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.user.dto.OpenShopRequest;
import com.helishop.core.modules.user.dto.ShopResponse;
import com.helishop.core.modules.user.service.ShopService;
import com.helishop.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller shop onboarding", description = "Mở và xem gian hàng của người bán hiện tại")
public class ShopController {
    private final ShopService shops;
    private final SecurityUtils security;

    @GetMapping("/me")
    @Operation(summary = "Gian hàng của tôi", description = "Lấy shop theo danh tính đăng nhập. Dữ liệu rỗng nếu tài khoản chưa mở shop.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thông tin shop hoặc dữ liệu rỗng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không phải người bán đang hoạt động")
    })
    public ApiResponse<ShopResponse> mine() {
        return ApiResponse.success(shops.mine(security.getCurrentUserId(null)), "Thông tin gian hàng của tôi");
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Mở gian hàng", description = "Một người bán có một shop. Máy chủ xác định owner từ token và trạng thái ACTIVE; không nhận ownerId hoặc status từ client.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đã mở gian hàng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tên hoặc mô tả không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không phải người bán đang hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tài khoản đã có shop hoặc tên shop bị trùng")
    })
    public ApiResponse<ShopResponse> open(@Valid @RequestBody OpenShopRequest request) {
        return ApiResponse.success(shops.open(security.getCurrentUserId(null), request), "Mở gian hàng thành công");
    }
}
