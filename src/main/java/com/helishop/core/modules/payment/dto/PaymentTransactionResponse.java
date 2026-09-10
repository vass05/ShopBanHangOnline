package com.helishop.core.modules.payment.dto;

import com.helishop.core.common.constants.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả giao dịch thanh toán gửi về cho người dùng")
public class PaymentTransactionResponse {

    @Schema(description = "Mã đơn hàng", example = "ORD-20260910-A1B2")
    private String orderCode;

    @Schema(description = "Mã giao dịch cổng thanh toán", example = "14567890")
    private String transactionCode;

    @Schema(description = "Cổng thanh toán", example = "VNPAY")
    private String paymentGateway;

    @Schema(description = "Số tiền giao dịch", example = "500000.00")
    private BigDecimal amount;

    @Schema(description = "Trạng thái thanh toán", example = "PAID")
    private PaymentStatus status;

    @Schema(description = "Mã phản hồi VNPAY (vnp_ResponseCode)", example = "00")
    private String responseCode;

    @Schema(description = "Thông điệp diễn giải kết quả giao dịch", example = "Giao dịch thanh toán thành công")
    private String message;

    @Schema(description = "Thời gian thực hiện giao dịch")
    private LocalDateTime transactionTime;
}
