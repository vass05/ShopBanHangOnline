package com.helishop.core.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin liên kết thanh toán VNPAY")
public class PaymentUrlResponse {

    @Schema(description = "Mã đơn hàng", example = "ORD-20260910-A1B2")
    private String orderCode;

    @Schema(description = "Số tiền thanh toán", example = "500000.00")
    private BigDecimal amount;

    @Schema(description = "Đường dẫn chuyển hướng tới cổng thanh toán VNPAY")
    private String paymentUrl;

    @Schema(description = "Thời điểm hết hạn của phiên thanh toán (định dạng yyyyMMddHHmmss)", example = "20260910153000")
    private String expireTime;
}
