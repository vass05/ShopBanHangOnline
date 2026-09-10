package com.helishop.core.notification;

import com.helishop.core.modules.notification.consumer.OrderEmailConsumer;
import com.helishop.core.modules.notification.service.EmailService;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEmailConsumerTest {

    @Mock
    private EmailService emailService;

    private OrderEmailConsumer orderEmailConsumer;

    @BeforeEach
    void setUp() {
        orderEmailConsumer = new OrderEmailConsumer(emailService);
    }

    @Test
    @DisplayName("Consumer nhận OrderPaidEvent và gọi EmailService gửi thư thành công")
    void testHandleOrderPaidEmail_Success() throws Exception {
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(1001L)
                .orderCode("ORD-20260910-A1B2")
                .customerEmail("customer@gmail.com")
                .totalAmount(BigDecimal.valueOf(500000))
                .paidAt(LocalDateTime.now())
                .items(List.of())
                .build();

        orderEmailConsumer.handleOrderPaidEmail(event);

        verify(emailService).sendOrderInvoiceEmail(event);
    }

    @Test
    @DisplayName("Consumer gặp lỗi khi gửi email -> Ném RuntimeException để kích hoạt cơ chế AMQP Retry x3")
    void testHandleOrderPaidEmail_ThrowsException_TriggersRetry() throws Exception {
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(1001L)
                .orderCode("ORD-FAIL-123")
                .customerEmail("fail@gmail.com")
                .build();

        doThrow(new MessagingException("SMTP Connection Timeout")).when(emailService).sendOrderInvoiceEmail(event);

        assertThatThrownBy(() -> orderEmailConsumer.handleOrderPaidEmail(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lỗi gửi email đơn hàng: ORD-FAIL-123");
    }
}
