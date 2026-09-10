package com.helishop.core.modules.product.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs quản lý danh mục sản phẩm đa tầng")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @Operation(
            summary = "Lấy cây danh mục sản phẩm đa tầng",
            description = "Truy xuất danh mục dạng cây đệ quy phân cấp cha - con (hỗ trợ cache Redis)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy cây danh mục thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<CategoryResponse>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTree());
    }

    @GetMapping
    @Operation(
            summary = "Lấy toàn bộ danh sách danh mục phẳng",
            description = "Trả về danh sách tất cả các danh mục trong hệ thống"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách danh mục phẳng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.success(categoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết danh mục theo ID",
            description = "Tìm kiếm thông tin danh mục theo khóa chính ID"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy thông tin danh mục",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục với ID yêu cầu",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getById(id));
    }
}
