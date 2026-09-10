package com.helishop.core.modules.notification.consumer;

import com.helishop.core.config.RabbitMQConfig;
import com.helishop.core.modules.notification.service.EmailService;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEmailConsumer {

    private final EmailService emailService;

    /**
     * Lắng nghe queue 'order.email.queue' để gửi email xác nhận thanh toán bất đồng bộ
     * Khi xảy ra lỗi (ví dụ: SMTP timeout, mạng chập chờn), ngoại lệ được ném ra để kích hoạt cơ chế Retry 3 lần
     * (Backoff: 2s -> 4s -> 8s). Sau 3 lần thất bại, message tự động chuyển sang DLQ ('order.email.dlq').
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_EMAIL)
    public void handleOrderPaidEmail(OrderPaidEvent event) {
        log.info("[RabbitMQ Worker] Nhận sự kiện OrderPaidEvent từ queue '{}' cho đơn hàng: '{}'",
                RabbitMQConfig.QUEUE_ORDER_EMAIL, event.getOrderCode());

        try {
            emailService.sendOrderInvoiceEmail(event);
        } catch (Exception e) {
            log.error("[RabbitMQ Worker] Gặp lỗi khi gửi email cho đơn hàng '{}': {}. Ném lỗi để kích hoạt AMQP Retry!",
                    event.getOrderCode(), e.getMessage());
            throw new RuntimeException("Lỗi gửi email đơn hàng: " + event.getOrderCode(), e);
        }
    }
}
