package com.helishop.core.modules.payment.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.payment.dto.CreatePaymentUrlRequest;
import com.helishop.core.modules.payment.dto.PaymentTransactionResponse;
import com.helishop.core.modules.payment.dto.PaymentUrlResponse;
import com.helishop.core.modules.payment.dto.VnPayIpnResponse;
import com.helishop.core.modules.payment.service.PaymentService;
import com.helishop.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Gateway Management", description = "APIs thanh toán trực tuyến qua cổng VNPAY Sandbox & Xử lý Webhook IPN Idempotent")
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    @PostMapping("/create-vnpay-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Khởi tạo URL thanh toán VNPAY Sandbox cho đơn hàng",
            description = "Tạo chuỗi tham số truy vấn, sắp xếp theo bảng chữ cái và sinh chữ ký số HMAC-SHA512 để điều hướng khách hàng sang cổng VNPAY"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sinh URL thanh toán thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Đơn hàng đã thanh toán hoặc dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<PaymentUrlResponse> createVnPayPaymentUrl(
            @RequestParam(required = false) Long customerId,
            @Valid @RequestBody CreatePaymentUrlRequest request,
            HttpServletRequest servletRequest) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        return ApiResponse.success(
                paymentService.createVnPayPaymentUrl(userId, request, servletRequest),
                "Khởi tạo liên kết thanh toán VNPAY thành công"
        );
    }

    @GetMapping("/vnpay-ipn")
    @Operation(
            summary = "Xử lý Webhook IPN từ VNPAY (Server-to-Server)",
            description = "Endpoint công khai nhận thông báo kết quả thanh toán từ VNPAY. Thực hiện: Xác minh Checksum HMAC-SHA512 -> Kiểm tra Idempotency -> Cập nhật trạng thái đơn hàng -> Lưu transaction -> Bắn sự kiện OrderPaidEvent vào RabbitMQ"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Phản hồi kết quả xử lý theo định dạng chuẩn của VNPAY ({RspCode, Message})",
                    content = @Content(schema = @Schema(implementation = VnPayIpnResponse.class)))
    })
    public VnPayIpnResponse handleVnPayIpn(@RequestParam Map<String, String> allParams) {
        return paymentService.processVnPayIpn(allParams);
    }

    @GetMapping("/vnpay-return")
    @Operation(
            summary = "Nhận kết quả điều hướng trở về từ cổng VNPAY (Client Return)",
            description = "Trang tiếp nhận người dùng quay trở lại hệ thống sau khi thanh toán trên cổng VNPAY, hiển thị thông báo trạng thái giao dịch"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xử lý kết quả trả về từ VNPAY thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<PaymentTransactionResponse> handleVnPayReturn(@RequestParam Map<String, String> allParams) {
        return ApiResponse.success(
                paymentService.processVnPayReturn(allParams),
                "Kết quả giao dịch thanh toán VNPAY"
        );
    }
}
