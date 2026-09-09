package com.helishop.core.modules.product.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.common.response.PageResponse;
import com.helishop.core.modules.product.dto.ProductFilterCriteria;
import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs quản lý hàng hóa, danh mục và tìm kiếm sản phẩm")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo ID")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo Slug")
    public ApiResponse<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ApiResponse.success(productService.getBySlug(slug));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm và lọc danh sách sản phẩm phân trang")
    public ApiResponse<PageResponse<ProductResponse>> searchProducts(ProductFilterCriteria criteria) {
        return ApiResponse.success(productService.searchProducts(criteria));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Thêm mới sản phẩm (Dành cho Người bán / Quản trị viên)")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.createProduct(request), "Tạo sản phẩm thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Cập nhật thông tin/giá sản phẩm (Dành cho Người bán / Quản trị viên - Tự động xóa Redis cache)")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.updateProduct(id, request), "Cập nhật sản phẩm thành công");
    }
}
