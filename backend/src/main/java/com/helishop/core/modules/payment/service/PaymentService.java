package com.helishop.core.modules.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.entity.PaymentTransaction;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.payment.config.VnPayConfig;
import com.helishop.core.modules.payment.dto.CreatePaymentUrlRequest;
import com.helishop.core.modules.payment.dto.PaymentTransactionResponse;
import com.helishop.core.modules.payment.dto.PaymentUrlResponse;
import com.helishop.core.modules.payment.dto.VnPayIpnResponse;
import com.helishop.core.modules.payment.event.OrderEventPublisher;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import com.helishop.core.modules.payment.repository.PaymentTransactionRepository;
import com.helishop.core.modules.payment.util.VnPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 1. Sinh URL thanh toán VNPAY Sandbox kèm chữ ký HMAC-SHA512
     */
    @Transactional(readOnly = true)
    public PaymentUrlResponse createVnPayPaymentUrl(Long userId, CreatePaymentUrlRequest request, HttpServletRequest servletRequest) {
        Order order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng với mã: " + request.getOrderCode()));

        if (order.getCustomer() != null && !order.getCustomer().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thanh toán cho đơn hàng này");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng này đã được thanh toán thành công trước đó");
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String createDate = now.format(VNPAY_DATE_FORMAT);
        String expireDate = now.plusMinutes(15).format(VNPAY_DATE_FORMAT);

        // Số tiền theo chuẩn VNPAY: VND nhân 100 và không có phần thập phân
        long vnpAmount = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderCode());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", (request.getLanguage() != null && !request.getLanguage().isBlank()) ? request.getLanguage() : "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VnPayUtils.getIpAddress(servletRequest));
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        if (request.getBankCode() != null && !request.getBankCode().isBlank()) {
            vnpParams.put("vnp_BankCode", request.getBankCode().trim());
        }

        // Tạo chữ ký bảo mật HMAC-SHA512
        String secureHash = VnPayUtils.hashAllFields(vnpParams, vnPayConfig.getHashSecret());
        String queryUrl = VnPayUtils.buildQueryUrl(vnpParams);
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;

        log.info("Đã tạo VNPAY Payment URL thành công cho đơn hàng '{}'", order.getOrderCode());

        return PaymentUrlResponse.builder()
                .orderCode(order.getOrderCode())
                .amount(order.getFinalAmount())
                .paymentUrl(paymentUrl)
                .expireTime(expireDate)
                .build();
    }

    /**
     * 2. Xử lý IPN (Instant Payment Notification - Webhook Server-to-Server)
     * Đảm bảo tính Idempotent tuyệt đối
     */
    @Transactional
    public VnPayIpnResponse processVnPayIpn(Map<String, String> allParams) {
        log.info("Nhận Webhook IPN từ VNPAY: {}", allParams);

        Map<String, String> fields = new HashMap<>(allParams);
        String vnpSecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // Bước 1: Validate Checksum (Xác minh chữ ký số tránh giả mạo)
        String signValue = VnPayUtils.hashAllFields(fields, vnPayConfig.getHashSecret());
        if (vnpSecureHash == null || !vnpSecureHash.equalsIgnoreCase(signValue)) {
            log.warn("Cảnh báo: Sai mã checksum VNPAY IPN! Nhận: '{}', Tính toán: '{}'", vnpSecureHash, signValue);
            return VnPayIpnResponse.invalidChecksum();
        }

        // Bước 2: Tìm kiếm thông tin đơn hàng
        String orderCode = fields.get("vnp_TxnRef");
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.warn("IPN thông báo cho đơn hàng không tồn tại: '{}'", orderCode);
            return VnPayIpnResponse.orderNotFound();
        }

        // Kiểm tra tính chính xác của số tiền thanh toán
        String amountStr = fields.get("vnp_Amount");
        long incomingAmount = amountStr != null ? Long.parseLong(amountStr) : 0L;
        long expectedAmount = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (incomingAmount != expectedAmount) {
            log.warn("IPN số tiền không khớp cho đơn hàng '{}'. Nhận: {}, Kỳ vọng: {}", orderCode, incomingAmount, expectedAmount);
            return VnPayIpnResponse.invalidAmount();
        }

        // Bước 3: Idempotency Check (Kiểm tra trạng thái đơn hàng để chống xử lý trùng lặp)
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Đơn hàng '{}' đã ở trạng thái PAID. Bỏ qua xử lý lặp lại (Idempotent response 02)", orderCode);
            return VnPayIpnResponse.orderAlreadyConfirmed();
        }

        // Bước 4: Cập nhật trạng thái và Lưu lịch sử giao dịch
        String responseCode = fields.get("vnp_ResponseCode");
        String transactionNo = fields.get("vnp_TransactionNo");
        String payDateStr = fields.get("vnp_PayDate");

        LocalDateTime transactionTime = parsePayDate(payDateStr);
        String gatewayResponseJson = serializeToJson(fields);

        String txCode = (transactionNo != null && !transactionNo.isBlank())
                ? transactionNo
                : orderCode + "_" + System.currentTimeMillis();

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .order(order)
                    .transactionCode(txCode)
                    .paymentGateway("VNPAY")
                    .amount(order.getFinalAmount())
                    .status(PaymentStatus.PAID)
                    .transactionTime(transactionTime)
                    .gatewayResponse(gatewayResponseJson)
                    .build();
            paymentTransactionRepository.save(transaction);

            log.info("Cập nhật đơn hàng '{}' sang PAID và PROCESSING thành công", orderCode);

            // Bước 5: Bắn sự kiện OrderPaidEvent vào RabbitMQ Exchange
            fireOrderPaidEvent(order, txCode, transactionTime);

            return VnPayIpnResponse.success();
        } else {
            // Thanh toán thất bại hoặc người dùng hủy giao dịch
            log.warn("Giao dịch VNPAY cho đơn hàng '{}' thất bại với mã lỗi: {}", orderCode, responseCode);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .order(order)
                    .transactionCode(txCode)
                    .paymentGateway("VNPAY")
                    .amount(order.getFinalAmount())
                    .status(PaymentStatus.FAILED)
                    .transactionTime(transactionTime)
                    .gatewayResponse(gatewayResponseJson)
                    .build();
            paymentTransactionRepository.save(transaction);

            return VnPayIpnResponse.success();
        }
    }

    /**
     * 3. Xử lý Return URL (Người dùng quay về sau thanh toán)
     */
    @Transactional(readOnly = true)
    public PaymentTransactionResponse processVnPayReturn(Map<String, String> allParams) {
        Map<String, String> fields = new HashMap<>(allParams);
        String vnpSecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String signValue = VnPayUtils.hashAllFields(fields, vnPayConfig.getHashSecret());
        boolean isValidHash = vnpSecureHash != null && vnpSecureHash.equalsIgnoreCase(signValue);

        String orderCode = fields.get("vnp_TxnRef");
        String transactionNo = fields.get("vnp_TransactionNo");
        String responseCode = fields.get("vnp_ResponseCode");
        String amountStr = fields.get("vnp_Amount");
        String payDateStr = fields.get("vnp_PayDate");

        BigDecimal amount = (amountStr != null)
                ? BigDecimal.valueOf(Long.parseLong(amountStr)).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        if (!isValidHash) {
            return PaymentTransactionResponse.builder()
                    .orderCode(orderCode)
                    .transactionCode(transactionNo)
                    .paymentGateway("VNPAY")
                    .amount(amount)
                    .status(PaymentStatus.FAILED)
                    .responseCode(responseCode)
                    .message("Chữ ký phản hồi không hợp lệ")
                    .transactionTime(parsePayDate(payDateStr))
                    .build();
        }

        boolean isSuccess = "00".equals(responseCode);
        return PaymentTransactionResponse.builder()
                .orderCode(orderCode)
                .transactionCode(transactionNo)
                .paymentGateway("VNPAY")
                .amount(amount)
                .status(isSuccess ? PaymentStatus.PAID : PaymentStatus.FAILED)
                .responseCode(responseCode)
                .message(isSuccess ? "Giao dịch thanh toán thành công" : "Giao dịch thanh toán không thành công (Mã lỗi: " + responseCode + ")")
                .transactionTime(parsePayDate(payDateStr))
                .build();
    }

    // --- Helper Methods ---

    private void fireOrderPaidEvent(Order order, String transactionCode, LocalDateTime paidAt) {
        List<OrderPaidEvent.PaidItemDto> items = order.getOrderItems().stream()
                .map(item -> OrderPaidEvent.PaidItemDto.builder()
                        .skuId(item.getProductSku() != null ? item.getProductSku().getId() : null)
                        .productTitle(item.getProductNameSnapshot())
                        .skuVariant(item.getSkuVariantSnapshot())
                        .price(item.getPriceAtPurchase())
                        .quantity(item.getQuantity())
                        .subTotal(item.getSubtotal())
                        .build())
                .toList();

        Long shopId = order.getShop() != null ? order.getShop().getId() : null;
        String shopName = order.getShop() != null ? order.getShop().getShopName() : "HeliShop Official";

        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .customerId(order.getCustomer().getId())
                .customerEmail(order.getCustomer().getEmail())
                .customerName(order.getCustomer().getFullName())
                .shopId(shopId)
                .shopName(shopName)
                .totalAmount(order.getFinalAmount())
                .paymentGateway("VNPAY")
                .transactionCode(transactionCode)
                .paidAt(paidAt)
                .shippingAddress(order.getShippingAddressSnapshot())
                .items(items)
                .build();

        orderEventPublisher.publishOrderPaidEvent(event);
    }

    private LocalDateTime parsePayDate(String payDateStr) {
        if (payDateStr != null && payDateStr.length() == 14) {
            try {
                return LocalDateTime.parse(payDateStr, VNPAY_DATE_FORMAT);
            } catch (Exception e) {
                log.warn("Không thể parse vnp_PayDate: {}", payDateStr);
            }
        }
        return LocalDateTime.now();
    }

    private String serializeToJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return map.toString();
        }
    }
}
