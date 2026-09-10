package com.helishop.core.modules.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Phản hồi chuẩn Webhook IPN cho hệ thống VNPAY")
public class VnPayIpnResponse {

    @JsonProperty("RspCode")
    @Schema(description = "Mã phản hồi kết quả xử lý IPN", example = "00")
    private String rspCode;

    @JsonProperty("Message")
    @Schema(description = "Thông điệp mô tả kết quả xử lý", example = "Confirm Success")
    private String message;

    public static VnPayIpnResponse success() {
        return new VnPayIpnResponse("00", "Confirm Success");
    }

    public static VnPayIpnResponse orderNotFound() {
        return new VnPayIpnResponse("01", "Order not found");
    }

    public static VnPayIpnResponse orderAlreadyConfirmed() {
        return new VnPayIpnResponse("02", "Order already confirmed");
    }

    public static VnPayIpnResponse invalidAmount() {
        return new VnPayIpnResponse("04", "Invalid Amount");
    }

    public static VnPayIpnResponse invalidChecksum() {
        return new VnPayIpnResponse("97", "Invalid Checksum");
    }

    public static VnPayIpnResponse unknownError(String msg) {
        return new VnPayIpnResponse("99", msg != null ? msg : "Unknown Error");
    }
}
