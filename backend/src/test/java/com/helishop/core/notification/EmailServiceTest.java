package com.helishop.core.notification;

import com.helishop.core.modules.notification.service.EmailService;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine);
    }

    @Test
    @DisplayName("Gửi email hóa đơn thành công, render đúng Thymeleaf template và gọi mailSender.send")
    void testSendOrderInvoiceEmail_Success() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/order-invoice"), any(Context.class))).thenReturn("<html>Invoice Content</html>");

        OrderPaidEvent.PaidItemDto item = OrderPaidEvent.PaidItemDto.builder()
                .skuId(105L)
                .productTitle("Củ sạc Anker 20W")
                .skuVariant("Màu Trắng")
                .price(BigDecimal.valueOf(250000))
                .quantity(2)
                .subTotal(BigDecimal.valueOf(500000))
                .build();

        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(1001L)
                .orderCode("ORD-20260910-A1B2")
                .customerEmail("customer@gmail.com")
                .customerName("Nguyen Van A")
                .shopName("Anker Official Store")
                .paymentGateway("VNPAY")
                .transactionCode("14567890")
                .shippingAddress("Hà Nội, Việt Nam")
                .paidAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(500000))
                .items(List.of(item))
                .build();

        emailService.sendOrderInvoiceEmail(event);

        verify(templateEngine).process(eq("mail/order-invoice"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }
}
