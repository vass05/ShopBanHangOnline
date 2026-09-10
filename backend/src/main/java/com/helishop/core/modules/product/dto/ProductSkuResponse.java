package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết của biến thể sản phẩm (SKU)")
public class ProductSkuResponse {

    @Schema(description = "ID biến thể SKU", example = "10")
    private Long id;

    @Schema(description = "ID sản phẩm cha", example = "100")
    private Long productId;

    @Schema(description = "Tên sản phẩm cha", example = "iPhone 16 Pro Max")
    private String productName;

    @Schema(description = "Mã SKU", example = "IP16PM-256GB-GOLD")
    private String skuCode;

    @Schema(description = "Giá bán hiện tại (VND)", example = "34990000")
    private BigDecimal price;

    @Schema(description = "Giá gốc niêm yết (VND)", example = "36990000")
    private BigDecimal originalPrice;

    @Schema(description = "Số lượng tồn kho thực tế", example = "15")
    private Integer stockQuantity;

    @Schema(description = "Thuộc tính biến thể dạng JSON", example = "{\"color\":\"Titan Sa Mạc\",\"storage\":\"256GB\"}")
    private String skuAttributes;

    @Schema(description = "Ảnh minh họa biến thể", example = "https://cdn.helishop.com/sku-gold.jpg")
    private String skuImageUrl;

    @Schema(description = "Phiên bản dữ liệu (Optimistic Locking version)", example = "0")
    private Long version;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;
}
