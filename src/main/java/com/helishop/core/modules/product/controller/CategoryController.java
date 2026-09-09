package com.helishop.core.modules.product.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Lấy cây danh mục sản phẩm đa tầng phân cấp (Category Tree)")
    public ApiResponse<List<CategoryResponse>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTree());
    }

    @GetMapping
    @Operation(summary = "Lấy toàn bộ danh sách danh mục phẳng")
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.success(categoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết danh mục theo ID")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getById(id));
    }
}
