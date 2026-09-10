package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết của sản phẩm")
public class ProductResponse {

    @Schema(description = "ID định danh sản phẩm", example = "100")
    private Long id;

    @Schema(description = "ID gian hàng", example = "1")
    private Long shopId;

    @Schema(description = "Tên gian hàng bán sản phẩm", example = "Apple Official Flagship Store")
    private String shopName;

    @Schema(description = "ID danh mục", example = "10")
    private Long categoryId;

    @Schema(description = "Tên danh mục", example = "Điện thoại thông minh")
    private String categoryName;

    @Schema(description = "Tên sản phẩm", example = "iPhone 16 Pro Max 256GB")
    private String name;

    @Schema(description = "Đường dẫn SEO (Slug)", example = "iphone-16-pro-max-256gb")
    private String slug;

    @Schema(description = "Mô tả sản phẩm", example = "Màu Titan Sa Mạc, bản quốc tế chính hãng...")
    private String description;

    @Schema(description = "URL ảnh chính", example = "https://cdn.helishop.com/iphone16-gold.jpg")
    private String mainImageUrl;

    @Schema(description = "Giá bán hiện tại (VND)", example = "34990000")
    private java.math.BigDecimal price;

    @Schema(description = "Đánh giá trung bình", example = "4.9")
    private java.math.BigDecimal rating;

    @Schema(description = "Số lượng còn lại trong kho", example = "25")
    private Integer stockQuantity;

    @Schema(description = "Trạng thái sản phẩm", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "Thời gian tạo sản phẩm")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private LocalDateTime updatedAt;
}
