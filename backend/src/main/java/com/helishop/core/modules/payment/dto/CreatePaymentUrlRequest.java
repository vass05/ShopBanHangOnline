package com.helishop.core.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu khởi tạo liên kết thanh toán qua cổng VNPAY")
public class CreatePaymentUrlRequest {

    @NotBlank(message = "Mã đơn hàng không được để trống")
    @Schema(description = "Mã công khai của đơn hàng (Order Code)", example = "ORD-20260910-A1B2")
    private String orderCode;

    @Schema(description = "Mã ngân hàng (tùy chọn, ví dụ: NCB, VCB, MB, VISA). Nếu để trống sẽ hiển thị toàn bộ phương thức trên cổng VNPAY", example = "NCB")
    private String bankCode;

    @Schema(description = "Ngôn ngữ giao diện thanh toán ('vn' hoặc 'en')", example = "vn")
    private String language;
}
