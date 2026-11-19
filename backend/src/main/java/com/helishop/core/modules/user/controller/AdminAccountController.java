package com.helishop.core.modules.user.controller;

import com.helishop.core.common.constants.*;
import com.helishop.core.common.response.*;
import com.helishop.core.modules.user.dto.*;
import com.helishop.core.modules.user.service.AdminAccountService;
import com.helishop.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/users") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin accounts", description = "Quản lý tài khoản người mua và người bán")
public class AdminAccountController {
    private final AdminAccountService service;
    private final SecurityUtils security;

    @GetMapping
    @Operation(summary = "Danh sách tài khoản người mua/người bán", description = "Phân trang 1-based, size tối đa 50, tìm email/họ tên, lọc vai trò/trạng thái. Không trả mật khẩu, token hoặc thông tin giao hàng.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách tài khoản"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bộ lọc sai"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc đã khóa"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cần admin đang hoạt động")})
    public ApiResponse<PageResponse<AdminAccountResponse>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) UserRole role, @RequestParam(required = false) UserStatus status) {
        return ApiResponse.success(service.list(security.getCurrentUserId(null), page, size, keyword, role, status));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Khóa/mở khóa người mua hoặc người bán", description = "Gửi expectedStatus và status ACTIVE/BLOCKED. Khóa bản ghi chống cập nhật đồng thời, thu hồi refresh token. Không cho tác động tới admin hoặc đổi vai trò.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trạng thái sau cập nhật"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Trạng thái không hợp lệ"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc đã khóa"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cần admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không phải tài khoản được quản lý"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Dữ liệu đã thay đổi")})
    public ApiResponse<AdminAccountResponse> update(@PathVariable Long id, @Valid @RequestBody AdminAccountStatusRequest request) {
        return ApiResponse.success(service.update(security.getCurrentUserId(null), id, request), "Đã cập nhật trạng thái tài khoản");
    }
}
