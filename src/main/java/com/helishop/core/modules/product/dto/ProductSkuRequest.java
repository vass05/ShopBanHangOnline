package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Yêu cầu tạo mới hoặc cập nhật biến thể sản phẩm (SKU)")
public class ProductSkuRequest {

    @Schema(description = "ID của sản phẩm cha", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @Schema(description = "Mã SKU duy nhất của biến thể", example = "IP16PM-256GB-GOLD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "SKU Code không được để trống")
    private String skuCode;

    @Schema(description = "Giá bán thực tế của biến thể (VND)", example = "34990000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    @Schema(description = "Giá gốc niêm yết trước khuyến mãi (VND)", example = "36990000")
    private BigDecimal originalPrice;

    @Schema(description = "Số lượng tồn kho của SKU", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @Schema(description = "Thuộc tính biến thể định dạng JSON", example = "{\"color\":\"Titan Sa Mạc\",\"storage\":\"256GB\"}")
    private String skuAttributes;

    @Schema(description = "Hình ảnh đại diện của biến thể", example = "https://cdn.helishop.com/sku-gold.jpg")
    private String skuImageUrl;
}
