package com.helishop.core.modules.product.controller;

import com.helishop.core.common.constants.ProductStatus;
import com.helishop.core.common.response.*;
import com.helishop.core.modules.product.dto.*;
import com.helishop.core.modules.product.service.SellerProductService;
import com.helishop.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/seller/products") @RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller products and SKUs", description = "Quản lý sản phẩm/biến thể trong gian hàng của tài khoản đăng nhập")
public class SellerProductController {
    private final SellerProductService service;
    private final SecurityUtils security;

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm của tôi", description = "Phân trang 1-based, tối đa 50/trang; gồm cả nháp và ẩn, lọc tên/trạng thái, không nhận shopId từ client")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách của shop hiện tại"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bộ lọc sai"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không phải seller/shop đang hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Chưa mở shop")})
    public ApiResponse<PageResponse<ProductResponse>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) ProductStatus status) {
        return ApiResponse.success(service.list(security.getCurrentUserId(null), page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sản phẩm của tôi", description = "Đọc dữ liệu mới nhất bao gồm SKU/version; không dùng cache công khai")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chi tiết sản phẩm và SKU"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sai chủ sở hữu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tồn tại")})
    public ApiResponse<ProductResponse> detail(@PathVariable Long id) { return ApiResponse.success(service.detail(security.getCurrentUserId(null), id)); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo sản phẩm và SKU", description = "Lưu nguyên tử 1–100 SKU cùng sản phẩm; shop lấy từ seller, slug tự sinh, giá/tổng kho tính từ SKU")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đã tạo sản phẩm và SKU"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Trùng mã/tổ hợp biến thể")})
    public ApiResponse<ProductResponse> create(@Valid @RequestBody SellerProductCreateRequest request) {
        return ApiResponse.success(service.create(security.getCurrentUserId(null), request), "Đã tạo sản phẩm");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa thông tin/ẩn hiện sản phẩm", description = "Thay tên, mô tả, danh mục, ảnh và trạng thái; không nhận giá/tổng kho hay shop mới")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã cập nhật sản phẩm"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sai chủ sở hữu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tồn tại")})
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody SellerProductRequest request) {
        return ApiResponse.success(service.update(security.getCurrentUserId(null), id, request), "Đã cập nhật sản phẩm");
    }

    @PostMapping("/{id}/skus") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm SKU", description = "Tối đa 100 SKU; mã duy nhất, cùng tên thuộc tính với các biến thể hiện có; tính lại giá và kho sau commit")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đã thêm SKU, trả sản phẩm mới nhất"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sai chủ sở hữu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Trùng mã/tổ hợp biến thể")})
    public ApiResponse<ProductResponse> addSku(@PathVariable Long id, @Valid @RequestBody SellerSkuRequest request) {
        return ApiResponse.success(service.saveSku(security.getCurrentUserId(null), id, null, request), "Đã thêm biến thể");
    }

    @PutMapping("/{id}/skus/{skuId}")
    @Operation(summary = "Sửa SKU và tồn kho", description = "Bắt buộc version đã đọc; version cũ trả 409 để không ghi đè thay đổi do checkout. Giữ ID SKU và đơn hàng lịch sử")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã cập nhật SKU, trả sản phẩm mới nhất"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sai chủ sở hữu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU không thuộc sản phẩm"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phiên bản cũ hoặc trùng mã/biến thể")})
    public ApiResponse<ProductResponse> updateSku(@PathVariable Long id, @PathVariable Long skuId, @Valid @RequestBody SellerSkuRequest request) {
        return ApiResponse.success(service.saveSku(security.getCurrentUserId(null), id, skuId, request), "Đã cập nhật biến thể");
    }
}
