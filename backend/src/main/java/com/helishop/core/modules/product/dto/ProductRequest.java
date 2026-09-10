package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin yêu cầu tạo mới hoặc cập nhật sản phẩm")
public class ProductRequest {

    @Schema(description = "ID của gian hàng sở hữu sản phẩm", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Shop ID không được để trống")
    private Long shopId;

    @Schema(description = "ID danh mục của sản phẩm", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Category ID không được để trống")
    private Long categoryId;

    @Schema(description = "Tên hiển thị của sản phẩm", example = "Điện thoại iPhone 16 Pro Max", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @Schema(description = "Đường dẫn thân thiện SEO (Slug)", example = "dien-thoai-iphone-16-pro-max")
    private String slug;

    @Schema(description = "Mô tả chi tiết sản phẩm", example = "Chip A18 Pro, Khung titan, Camera 48MP...")
    private String description;

    @Schema(description = "Đường dẫn ảnh đại diện chính", example = "https://cdn.helishop.com/iphone-16.jpg")
    private String mainImageUrl;

    @Schema(description = "Giá cơ bản của sản phẩm (VND)", example = "34990000")
    private java.math.BigDecimal price;

    @Schema(description = "Điểm đánh giá trung bình (1-5 sao)", example = "4.9")
    private java.math.BigDecimal rating;

    @Schema(description = "Tổng số lượng tồn kho của sản phẩm", example = "50")
    private Integer stockQuantity;

    @Schema(description = "Trạng thái hiển thị của sản phẩm (DRAFT, ACTIVE, INACTIVE, OUT_OF_STOCK)", example = "ACTIVE")
    private ProductStatus status;
}
