package com.helishop.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.entity.OrderItem;
import com.helishop.core.modules.order.entity.PaymentTransaction;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.payment.config.VnPayConfig;
import com.helishop.core.modules.payment.dto.CreatePaymentUrlRequest;
import com.helishop.core.modules.payment.dto.PaymentUrlResponse;
import com.helishop.core.modules.payment.dto.VnPayIpnResponse;
import com.helishop.core.modules.payment.event.OrderEventPublisher;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import com.helishop.core.modules.payment.repository.PaymentTransactionRepository;
import com.helishop.core.modules.payment.service.PaymentService;
import com.helishop.core.modules.payment.util.VnPayUtils;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private VnPayConfig vnPayConfig;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentService paymentService;

    private Order testOrder;
    private final String secretKey = "404E635266556A586E3272357538782F";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                vnPayConfig,
                orderRepository,
                paymentTransactionRepository,
                orderEventPublisher,
                objectMapper
        );

        User customer = User.builder()
                .email("customer@gmail.com")
                .fullName("Nguyen Van A")
                .build();
        customer.setId(1L);

        Shop shop = Shop.builder()
                .shopName("Anker Official Store")
                .build();
        shop.setId(3L);

        Product product = Product.builder()
                .name("Củ sạc nhanh Anker 20W GaN")
                .build();
        product.setId(42L);

        ProductSku sku = ProductSku.builder()
                .product(product)
                .skuCode("ANKER-20W")
                .skuAttributes("Trắng")
                .price(BigDecimal.valueOf(250000))
                .build();
        sku.setId(105L);

        OrderItem item = OrderItem.builder()
                .productSku(sku)
                .productNameSnapshot("Củ sạc nhanh Anker 20W GaN")
                .skuVariantSnapshot("Trắng")
                .priceAtPurchase(BigDecimal.valueOf(250000))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(500000))
                .build();

        testOrder = Order.builder()
                .orderCode("ORD-20260910-A1B2")
                .customer(customer)
                .shop(shop)
                .totalAmount(BigDecimal.valueOf(500000))
                .finalAmount(BigDecimal.valueOf(500000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingAddressSnapshot("Hà Nội")
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        testOrder.setId(1001L);
    }

    @Test
    @DisplayName("Khởi tạo VNPAY Payment URL thành công kèm chữ ký HMAC-SHA512")
    void testCreateVnPayPaymentUrl_Success() {
        CreatePaymentUrlRequest request = CreatePaymentUrlRequest.builder()
                .orderCode("ORD-20260910-A1B2")
                .bankCode("NCB")
                .language("vn")
                .build();

        when(orderRepository.findByOrderCode("ORD-20260910-A1B2")).thenReturn(Optional.of(testOrder));
        when(vnPayConfig.getTmnCode()).thenReturn("HELI0001");
        when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
        when(vnPayConfig.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:8080/api/v1/payments/vnpay-return");

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");

        PaymentUrlResponse response = paymentService.createVnPayPaymentUrl(1L, request, servletRequest);

        assertThat(response).isNotNull();
        assertThat(response.getOrderCode()).isEqualTo("ORD-20260910-A1B2");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(response.getPaymentUrl()).contains("vnp_SecureHash=");
        assertThat(response.getPaymentUrl()).contains("vnp_Amount=50000000"); // 500,000 * 100
        assertThat(response.getPaymentUrl()).contains("vnp_BankCode=NCB");
        assertThat(response.getPaymentUrl()).contains("vnp_TmnCode=HELI0001");
    }

    @Test
    @DisplayName("Khởi tạo URL cho đơn hàng đã thanh toán -> Báo lỗi INVALID_REQUEST")
    void testCreateVnPayPaymentUrl_AlreadyPaid_ThrowsException() {
        testOrder.setPaymentStatus(PaymentStatus.PAID);
        CreatePaymentUrlRequest request = CreatePaymentUrlRequest.builder()
                .orderCode("ORD-20260910-A1B2")
                .build();

        when(orderRepository.findByOrderCode("ORD-20260910-A1B2")).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> paymentService.createVnPayPaymentUrl(1L, request, new MockHttpServletRequest()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("IPN: Chữ ký không hợp lệ -> Trả về mã 97 (Invalid Checksum)")
    void testProcessVnPayIpn_InvalidChecksum() {
        when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-20260910-A1B2");
        params.put("vnp_Amount", "50000000");
        params.put("vnp_SecureHash", "INVALID_HASH_VALUE");

        VnPayIpnResponse response = paymentService.processVnPayIpn(params);

        assertThat(response.getRspCode()).isEqualTo("97");
        assertThat(response.getMessage()).isEqualTo("Invalid Checksum");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("IPN: Không tìm thấy đơn hàng -> Trả về mã 01 (Order not found)")
    void testProcessVnPayIpn_OrderNotFound() {
        when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "ORD-NONEXISTENT");
        fields.put("vnp_Amount", "50000000");

        String validHash = VnPayUtils.hashAllFields(fields, secretKey);
        fields.put("vnp_SecureHash", validHash);

        when(orderRepository.findByOrderCode("ORD-NONEXISTENT")).thenReturn(Optional.empty());

        VnPayIpnResponse response = paymentService.processVnPayIpn(fields);

        assertThat(response.getRspCode()).isEqualTo("01");
        assertThat(response.getMessage()).isEqualTo("Order not found");
    }

    @Test
    @DisplayName("IPN: Số tiền không khớp với đơn hàng -> Trả về mã 04 (Invalid Amount)")
    void testProcessVnPayIpn_InvalidAmount() {
        when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "ORD-20260910-A1B2");
        fields.put("vnp_Amount", "99999999"); // Sai số tiền (kỳ vọng 50,000,000)

        String validHash = VnPayUtils.hashAllFields(fields, secretKey);
        fields.put("vnp_SecureHash", validHash);

        when(orderRepository.findByOrderCode("ORD-20260910-A1B2")).thenReturn(Optional.of(testOrder));

        VnPayIpnResponse response = paymentService.processVnPayIpn(fields);

        assertThat(response.getRspCode()).isEqualTo("04");
        assertThat(response.getMessage()).isEqualTo("Invalid Amount");
    }

    @Test
    @DisplayName("IPN: Idempotency Check - Đơn hàng đã ở trạng thái PAID -> Trả ngay mã 02 (Order already confirmed)")
    void testProcessVnPayIpn_Idempotent_AlreadyPaid() {
        testOrder.setPaymentStatus(PaymentStatus.PAID);
        when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "ORD-20260910-A1B2");
        fields.put("vnp_Amount", "50000000");

        String validHash = VnPayUtils.hashAllFields(fields, secretKey);
        fields.put("vnp_SecureHash", validHash);

        when(orderRepository.findByOrderCode("ORD-20260910-A1B2")).thenReturn(Optional.of(testOrder));

        VnPayIpnResponse response = paymentService.processVnPayIpn(fields);

        assertThat(response.getRspCode()).isEqualTo("02");
        assertThat(response.getMessage()).isEqualTo("Order already confirmed");

        // Đảm bảo không ghi đè DB hay bắn event lặp lại
        verify(orderRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(orderEventPublisher, never()).publishOrderPaidEvent(any());
    }

    @Test
    @DisplayName("IPN: Thanh toán thành công (vnp_ResponseCode=00) -> Cập nhật PAID, PROCESSING, lưu Transaction và bắn RabbitMQ Event")
    void testProcessVnPayIpn_Success_Flow() {
        when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "ORD-20260910-A1B2");
        fields.put("vnp_Amount", "50000000");
        fields.put("vnp_ResponseCode", "00");
        fields.put("vnp_TransactionNo", "14567890");
        fields.put("vnp_PayDate", "20260910153000");

        String validHash = VnPayUtils.hashAllFields(fields, secretKey);
        fields.put("vnp_SecureHash", validHash);

        when(orderRepository.findByOrderCode("ORD-20260910-A1B2")).thenReturn(Optional.of(testOrder));

        VnPayIpnResponse response = paymentService.processVnPayIpn(fields);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(response.getMessage()).isEqualTo("Confirm Success");

        // Kiểm tra cập nhật trạng thái đơn hàng
        assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(testOrder);

        // Kiểm tra lưu transaction
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        PaymentTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getTransactionCode()).isEqualTo("14567890");
        assertThat(savedTx.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedTx.getPaymentGateway()).isEqualTo("VNPAY");

        // Kiểm tra bắn sự kiện RabbitMQ OrderPaidEvent
        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(orderEventPublisher).publishOrderPaidEvent(eventCaptor.capture());
        OrderPaidEvent event = eventCaptor.getValue();
        assertThat(event.getOrderCode()).isEqualTo("ORD-20260910-A1B2");
        assertThat(event.getCustomerEmail()).isEqualTo("customer@gmail.com");
        assertThat(event.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(event.getItems()).hasSize(1);
    }
}
