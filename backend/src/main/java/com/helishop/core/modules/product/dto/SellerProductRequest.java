package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Thông tin sản phẩm của người bán; giá và tổng kho được tính từ SKU")
public record SellerProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Positive Long categoryId,
        @Size(max = 10000) String description,
        @Size(max = 500) String mainImageUrl,
        @NotNull ProductStatus status) {}
