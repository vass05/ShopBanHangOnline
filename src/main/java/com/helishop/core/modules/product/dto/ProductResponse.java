package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
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
public class ProductResponse {

    private Long id;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String slug;
    private String description;
    private String mainImageUrl;
    private java.math.BigDecimal price;
    private java.math.BigDecimal rating;
    private Integer stockQuantity;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
