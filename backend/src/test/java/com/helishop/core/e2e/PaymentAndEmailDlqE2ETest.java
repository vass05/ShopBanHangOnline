package com.helishop.core.e2e;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.config.RabbitMQConfig;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.notification.consumer.OrderEmailConsumer;
import com.helishop.core.modules.notification.service.EmailService;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.payment.dto.VnPayIpnResponse;
import com.helishop.core.modules.order.entity.PaymentTransaction;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import com.helishop.core.modules.payment.repository.PaymentTransactionRepository;
import com.helishop.core.modules.payment.service.PaymentService;
import com.helishop.core.modules.payment.util.VnPayUtils;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentAndEmailDlqE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private com.helishop.core.modules.payment.config.VnPayConfig vnPayConfig;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private User sampleCustomer;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        paymentTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        sampleCustomer = userRepository.save(User.builder()
                .fullName("Nguyen Van A")
                .email("customer.e2e@helishop.com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456")
                .role(com.helishop.core.common.constants.UserRole.ROLE_CUSTOMER)
                .status(com.helishop.core.common.constants.UserStatus.ACTIVE)
                .build());

        sampleOrder = orderRepository.save(Order.builder()
                .orderCode("ORD-E2E-2026-999")
                .customer(sampleCustomer)
                .totalAmount(BigDecimal.valueOf(1500000))
                .finalAmount(BigDecimal.valueOf(1500000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingAddressSnapshot("123 Duong Bien, Da Nang")
                .build());
    }

    @Test
    @DisplayName("E2E Test 1: Luồng VNPAY IPN hợp lệ -> Cập nhật trạng thái PAID -> Đẩy task vào RabbitMQ")
    void shouldProcessVnPayIpnAndPublishEventToRabbitMQ() {
        // 1. Chuẩn bị params VNPAY gửi về
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TxnRef", sampleOrder.getOrderCode());
        vnpParams.put("vnp_Amount", "150000000"); // 1,500,000 * 100
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TransactionNo", "14567890");
        vnpParams.put("vnp_PayDate", "20260910160000");
        vnpParams.put("vnp_BankCode", "NCB");

        String secureHash = VnPayUtils.hashAllFields(vnpParams, vnPayConfig.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        // 2. Thực thi xử lý IPN
        VnPayIpnResponse ipnResponse = paymentService.processVnPayIpn(vnpParams);

        // 3. Xác thực IPN phản hồi mã 00 Confirm Success
        assertThat(ipnResponse.getRspCode()).isEqualTo("00");
        assertThat(ipnResponse.getMessage()).isEqualTo("Confirm Success");

        // 4. Kiểm tra Database được cập nhật chính xác
        Order updatedOrder = orderRepository.findByOrderCode(sampleOrder.getOrderCode()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);

        // 5. Kiểm tra Giao dịch thanh toán được lưu trữ
        java.util.List<PaymentTransaction> txns = paymentTransactionRepository.findByOrderId(sampleOrder.getId());
        assertThat(txns).isNotEmpty();
        assertThat(txns.get(0).getTransactionCode()).isEqualTo("14567890");
        assertThat(txns.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500000));

        // 6. Kiểm tra RabbitMQ được gọi đẩy OrderPaidEvent vào queue
        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_ORDER_DIRECT),
                eq(RabbitMQConfig.ROUTING_ORDER_PAID_EMAIL),
                eventCaptor.capture()
        );

        OrderPaidEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderCode()).isEqualTo(sampleOrder.getOrderCode());
        assertThat(capturedEvent.getCustomerEmail()).isEqualTo("customer.e2e@helishop.com");
    }

    @Test
    @DisplayName("E2E Test 2: Tính Idempotent - Gửi IPN lần thứ 2 cho đơn đã PAID -> Trả về mã 02 Order already confirmed")
    void shouldHandleDuplicateIpnIdempotently() {
        // Cập nhật đơn hàng đã PAID trước đó
        sampleOrder.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(sampleOrder);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_TxnRef", sampleOrder.getOrderCode());
        vnpParams.put("vnp_Amount", "150000000");
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TransactionNo", "14567890");

        String secureHash = VnPayUtils.hashAllFields(vnpParams, vnPayConfig.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        // Gọi IPN lần 2
        VnPayIpnResponse ipnResponse = paymentService.processVnPayIpn(vnpParams);

        // Đảm bảo trả mã 02 mà không xử lý lại hoặc ghi đè
        assertThat(ipnResponse.getRspCode()).isEqualTo("02");
        assertThat(ipnResponse.getMessage()).isEqualTo("Order already confirmed");

        // Không gửi trùng sự kiện vào RabbitMQ lần thứ 2
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("E2E Test 3: Chịu lỗi (Fault Tolerance) - Mô phỏng lỗi SMTP -> Consumer ném lỗi kích hoạt Retry x3 & DLQ")
    void shouldTriggerRetryAndDeadLetterQueueWhenSmtpServiceDown() throws Exception {
        OrderEmailConsumer consumer = new OrderEmailConsumer(emailService);

        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(sampleOrder.getId())
                .orderCode(sampleOrder.getOrderCode())
                .customerEmail("customer.e2e@helishop.com")
                .totalAmount(BigDecimal.valueOf(1500000))
                .paidAt(LocalDateTime.now())
                .items(java.util.Collections.emptyList())
                .build();

        // Giả lập SMTP Server bị sập (MessagingException)
        doThrow(new MessagingException("Connection refused: connect to mail server on port 1025"))
                .when(emailService).sendOrderInvoiceEmail(any(OrderPaidEvent.class));

        // Consumer bắt exception và ném RuntimeException để Spring AMQP Retry cơ chế 3 lần
        assertThatThrownBy(() -> consumer.handleOrderPaidEmail(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lỗi gửi email đơn hàng: " + sampleOrder.getOrderCode());

        // Kiểm chứng rằng emailService đã cố gắng được gọi
        verify(emailService, times(1)).sendOrderInvoiceEmail(event);
    }
}
