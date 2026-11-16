package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "Tạo sản phẩm và các SKU trong cùng một giao dịch")
public record SellerProductCreateRequest(@NotNull @Valid SellerProductRequest product,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid SellerSkuRequest> skus) {}
