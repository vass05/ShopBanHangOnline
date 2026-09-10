package com.helishop.core.modules.notification.consumer;

import com.helishop.core.config.RabbitMQConfig;
import com.helishop.core.modules.payment.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderDlqConsumer {

    /**
     * Lắng nghe Dead Letter Queue 'order.email.dlq'
     * Ghi log cảnh báo và lưu trữ các thông điệp gửi email thất bại sau toàn bộ số lần thử lại (Retry x3)
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_EMAIL_DLQ)
    public void handleDeadLetterMessage(OrderPaidEvent event) {
        log.error("[DLQ ALERT] Phát hiện thông điệp lỗi trong Dead Letter Queue '{}'! Mã đơn hàng: '{}', Email khách hàng: '{}', Số tiền: '{}'",
                RabbitMQConfig.QUEUE_ORDER_EMAIL_DLQ,
                event.getOrderCode(),
                event.getCustomerEmail(),
                event.getTotalAmount());
    }
}
