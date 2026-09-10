package com.helishop.core.modules.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent implements Serializable {

    private Long orderId;
    private String orderCode;
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private Long shopId;
    private String shopName;
    private BigDecimal totalAmount;
    private String paymentGateway;
    private String transactionCode;
    private LocalDateTime paidAt;
    private List<PaidItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaidItemDto implements Serializable {
        private Long skuId;
        private String productTitle;
        private String skuVariant;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subTotal;
    }
}
