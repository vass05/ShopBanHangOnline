package com.helishop.core.modules.notification.service;

import com.helishop.core.modules.payment.event.OrderPaidEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:helishop.system@gmail.com}")
    private String senderEmail = "helishop.system@gmail.com";

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Render template HTML Thymeleaf và gửi email hóa đơn đơn hàng qua JavaMailSender
     */
    public void sendOrderInvoiceEmail(OrderPaidEvent event) throws MessagingException {
        log.info("Bắt đầu gửi email hóa đơn cho đơn hàng '{}' tới '{}'", event.getOrderCode(), event.getCustomerEmail());

        Context context = new Context();
        context.setVariable("customerName", event.getCustomerName() != null ? event.getCustomerName() : "Quý khách");
        context.setVariable("orderCode", event.getOrderCode());
        context.setVariable("shopName", event.getShopName() != null ? event.getShopName() : "HeliShop Official");
        context.setVariable("paymentGateway", event.getPaymentGateway() != null ? event.getPaymentGateway() : "VNPAY");
        context.setVariable("transactionCode", event.getTransactionCode());
        context.setVariable("shippingAddress", event.getShippingAddress());
        context.setVariable("paidAt", event.getPaidAt() != null ? event.getPaidAt().format(DISPLAY_FORMAT) : "");
        context.setVariable("items", event.getItems());
        context.setVariable("totalAmount", event.getTotalAmount());

        String htmlContent = templateEngine.process("mail/order-invoice", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(event.getCustomerEmail());
        helper.setSubject(String.format("HeliShop - Xác nhận thanh toán thành công đơn hàng #%s", event.getOrderCode()));
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
        log.info("Gửi email xác nhận thanh toán thành công cho đơn hàng '{}' tới email '{}'", event.getOrderCode(), event.getCustomerEmail());
    }
}
