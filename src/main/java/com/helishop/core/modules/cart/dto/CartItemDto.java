package com.helishop.core.modules.cart.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItemDto implements Serializable {

    private Long skuId;
    private Long productId;
    private Long shopId;
    private String shopName;
    private String productTitle;
    private String skuVariant;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
    private Integer stockAvailable;
    private Long updatedAt;

    public BigDecimal getSubTotal() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
