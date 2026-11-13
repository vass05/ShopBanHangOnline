package com.helishop.core.modules.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.common.constants.PaymentMethod;
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
    @Transactional
    public PaymentUrlResponse createVnPayPaymentUrl(Long userId, CreatePaymentUrlRequest request, HttpServletRequest servletRequest) {
        if (vnPayConfig.isDisabled()) throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE);
        Order order = orderRepository.findByOrderCodeWithLock(request.getOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng với mã: " + request.getOrderCode()));

        if (order.getCustomer() == null || !order.getCustomer().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thanh toán cho đơn hàng này");
        }

        if (order.getPaymentMethod() != PaymentMethod.VNPAY || order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == OrderStatus.RETURNED || order.getOrderStatus() == OrderStatus.DELIVERED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED || order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng này đã được thanh toán thành công trước đó");
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String createDate = now.format(VNPAY_DATE_FORMAT);
        String expireDate = now.plusMinutes(15).format(VNPAY_DATE_FORMAT);

        // Số tiền theo chuẩn VNPAY: VND nhân 100 và không có phần thập phân
        long vnpAmount;
        try { vnpAmount = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValueExact(); }
        catch (ArithmeticException exception) { throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền thanh toán không hợp lệ"); }
        if (vnpAmount <= 0) throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền thanh toán phải lớn hơn 0");

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
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentExpiresAt(now.plusMinutes(15).toLocalDateTime());
        orderRepository.save(order);

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
        if (vnPayConfig.isDisabled()) return VnPayIpnResponse.unknownError("Payment gateway is not configured");
        Map<String, String> fields = signedFields(allParams);
        if (!validSignature(allParams, fields) || !vnPayConfig.getTmnCode().equals(fields.get("vnp_TmnCode"))) return VnPayIpnResponse.invalidChecksum();
        String orderCode = fields.get("vnp_TxnRef");
        if (orderCode == null || orderCode.isBlank()) return VnPayIpnResponse.orderNotFound();
        Order order = orderRepository.findByOrderCodeWithLock(orderCode).orElse(null);
        if (order == null) return VnPayIpnResponse.orderNotFound();
        if (order.getPaymentMethod() != PaymentMethod.VNPAY) return VnPayIpnResponse.unknownError("Payment method mismatch");
        BigDecimal incomingAmount = callbackAmount(fields.get("vnp_Amount"));
        if (incomingAmount == null || order.getFinalAmount().compareTo(incomingAmount) != 0) return VnPayIpnResponse.invalidAmount();
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.REFUNDED) return VnPayIpnResponse.orderAlreadyConfirmed();
        String responseCode = fields.get("vnp_ResponseCode");
        String gatewayStatus = fields.get("vnp_TransactionStatus");
        if (responseCode == null || !responseCode.matches("[0-9]{2}") || gatewayStatus == null || !gatewayStatus.matches("[0-9]{2}")) return VnPayIpnResponse.unknownError("Invalid callback fields");
        boolean success = "00".equals(responseCode) && "00".equals(gatewayStatus);
        String transactionNo = fields.get("vnp_TransactionNo");
        if (success && (transactionNo == null || !transactionNo.matches("[0-9]{1,30}") || transactionNo.matches("0+"))) return VnPayIpnResponse.unknownError("Invalid transaction number");
        String txCode = transactionNo != null && transactionNo.matches("[0-9]{1,30}") && !transactionNo.matches("0+")
                ? transactionNo : orderCode + "_" + VnPayUtils.hashAllFields(fields, vnPayConfig.getHashSecret()).substring(0, 24);
        var existing = paymentTransactionRepository.findByTransactionCode(txCode);
        PaymentStatus target = success ? PaymentStatus.PAID : PaymentStatus.FAILED;
        if (existing.isPresent()) {
            if (!existing.get().getOrder().getId().equals(order.getId())) return VnPayIpnResponse.unknownError("Transaction belongs to another order");
            if (existing.get().getStatus() == target) return VnPayIpnResponse.orderAlreadyConfirmed();
        }
        LocalDateTime paidAt = parsePayDate(fields.get("vnp_PayDate"));
        PaymentTransaction transaction = existing.orElseGet(() -> PaymentTransaction.builder().order(order).transactionCode(txCode).paymentGateway("VNPAY").build());
        transaction.setAmount(order.getFinalAmount());
        transaction.setStatus(target);
        transaction.setTransactionTime(paidAt);
        transaction.setGatewayResponse(serializeToJson(fields));
        paymentTransactionRepository.save(transaction);
        order.setPaymentStatus(target);
        boolean closed = order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.RETURNED;
        if (success && !closed && (order.getOrderStatus() == OrderStatus.PENDING || order.getOrderStatus() == OrderStatus.CONFIRMED)) order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        if (success && !closed) fireOrderPaidEvent(order, txCode, paidAt);
        if (success && closed) log.warn("Thanh toán đến sau khi đóng đơn {}; cần đối soát/hoàn tiền, không kích hoạt lại đơn", orderCode);
        return VnPayIpnResponse.success();
    }

    @Transactional(readOnly = true)
    public PaymentTransactionResponse processVnPayReturn(Map<String, String> allParams) {
        if (vnPayConfig.isDisabled()) throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE);
        Map<String, String> fields = signedFields(allParams);
        if (!validSignature(allParams, fields) || !vnPayConfig.getTmnCode().equals(fields.get("vnp_TmnCode"))) return invalidReturn("Chữ ký phản hồi không hợp lệ");
        String orderCode = fields.get("vnp_TxnRef");
        Order order = orderCode != null ? orderRepository.findByOrderCode(orderCode).orElse(null) : null;
        BigDecimal amount = callbackAmount(fields.get("vnp_Amount"));
        if (order == null || order.getPaymentMethod() != PaymentMethod.VNPAY || amount == null || amount.compareTo(order.getFinalAmount()) != 0) return invalidReturn("Đơn hàng hoặc số tiền phản hồi không hợp lệ");
        boolean gatewaySuccess = "00".equals(fields.get("vnp_ResponseCode")) && "00".equals(fields.get("vnp_TransactionStatus"));
        PaymentStatus status = order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.REFUNDED
                ? order.getPaymentStatus() : gatewaySuccess ? PaymentStatus.PENDING : PaymentStatus.FAILED;
        return PaymentTransactionResponse.builder().verified(true).orderCode(orderCode).paymentGateway("VNPAY")
                .transactionCode(fields.get("vnp_TransactionNo")).amount(order.getFinalAmount()).status(status)
                .responseCode(fields.get("vnp_ResponseCode")).transactionTime(parsePayDate(fields.get("vnp_PayDate")))
                .message(status == PaymentStatus.PAID ? "Đã nhận xác nhận thanh toán từ máy chủ"
                        : gatewaySuccess ? "Đang chờ xác nhận thanh toán từ máy chủ" : "Giao dịch chưa thành công").build();
    }

    private Map<String, String> signedFields(Map<String, String> allParams) {
        Map<String, String> fields = new HashMap<>(allParams);
        fields.remove("vnp_SecureHash"); fields.remove("vnp_SecureHashType");
        return fields;
    }

    private boolean validSignature(Map<String, String> allParams, Map<String, String> fields) {
        String expected = VnPayUtils.hashAllFields(fields, vnPayConfig.getHashSecret());
        String received = allParams.get("vnp_SecureHash");
        return received != null && java.security.MessageDigest.isEqual(
                expected.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                received.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private BigDecimal callbackAmount(String amount) {
        if (amount == null || !amount.matches("[0-9]{1,18}")) return null;
        return new BigDecimal(amount).divide(BigDecimal.valueOf(100));
    }

    private PaymentTransactionResponse invalidReturn(String message) {
        return PaymentTransactionResponse.builder().verified(false).status(PaymentStatus.FAILED).amount(BigDecimal.ZERO).message(message).build();
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
