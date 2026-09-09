package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
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
public class ProductRequest {

    @NotNull(message = "Shop ID không được để trống")
    private Long shopId;

    @NotNull(message = "Category ID không được để trống")
    private Long categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String slug;

    private String description;

    private String mainImageUrl;

    private java.math.BigDecimal price;

    private java.math.BigDecimal rating;

    private ProductStatus status;
}
