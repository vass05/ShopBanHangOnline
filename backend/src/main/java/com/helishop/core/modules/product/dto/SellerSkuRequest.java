package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Biến thể; khi cập nhật phải gửi version đã đọc, gồm cả khi thay đổi giá/kho")
public record SellerSkuRequest(
        @NotBlank @Size(max = 100) String skuCode,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2) BigDecimal price,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2) BigDecimal originalPrice,
        @NotNull @Min(0) @Max(1000000) Integer stockQuantity,
        @Size(max = 2) Map<@NotBlank @Size(max = 50) String, @NotBlank @Size(max = 100) String> attributes,
        @Size(max = 500) String skuImageUrl,
        @Min(0) Long version) {}
