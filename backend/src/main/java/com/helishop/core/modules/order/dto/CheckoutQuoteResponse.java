package com.helishop.core.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Báo giá do server tính lại theo SKU, shop, vận chuyển và voucher")
public record CheckoutQuoteResponse(BigDecimal totalAmount, BigDecimal shippingFee, BigDecimal discountAmount,
                                    BigDecimal finalAmount, List<ShopQuote> shops) {
    public record ShopQuote(Long shopId, String shopName, BigDecimal totalAmount, BigDecimal shippingFee,
                            BigDecimal discountAmount, BigDecimal finalAmount, List<Item> items) {}
    public record Item(Long skuId, Long productId, String productName, String skuCode, String imageUrl,
                       String skuVariant, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
