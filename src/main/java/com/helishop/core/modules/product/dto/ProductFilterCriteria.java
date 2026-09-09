package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tiêu chí tìm kiếm và lọc động danh sách sản phẩm")
public class ProductFilterCriteria {

    @Schema(description = "Từ khóa tìm kiếm theo tên hoặc mô tả", example = "iPhone")
    private String keyword;

    @Schema(description = "ID danh mục (sẽ tự động lọc cả các danh mục con cháu)", example = "1")
    private Long categoryId;

    @Schema(description = "ID cửa hàng / gian hàng bán", example = "1")
    private Long shopId;

    @Schema(description = "Trạng thái sản phẩm", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "Khoảng giá tối thiểu (VNĐ)", example = "10000000")
    private BigDecimal minPrice;

    @Schema(description = "Khoảng giá tối đa (VNĐ)", example = "35000000")
    private BigDecimal maxPrice;

    @Schema(description = "Đánh giá tối thiểu (sao từ 1.0 đến 5.0)", example = "4.5")
    private BigDecimal minRating;

    @Builder.Default
    @Schema(description = "Số trang hiển thị (bắt đầu từ 1)", example = "1")
    private int page = 1;

    @Builder.Default
    @Schema(description = "Kích thước mỗi trang", example = "20")
    private int size = 20;

    @Builder.Default
    @Schema(description = "Trường sắp xếp (id, price, createdAt, rating)", example = "id")
    private String sortBy = "id";

    @Builder.Default
    @Schema(description = "Hướng sắp xếp (ASC hoặc DESC)", example = "DESC")
    private String sortDirection = "DESC";
}
