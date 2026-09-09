package com.helishop.core.modules.product.dto;

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
public class ProductSkuResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String skuCode;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private String skuAttributes;
    private String skuImageUrl;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
