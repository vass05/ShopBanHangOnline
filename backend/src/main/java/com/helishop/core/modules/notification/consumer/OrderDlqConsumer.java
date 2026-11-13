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
     * Listener chẩn đoán được giữ ở trạng thái dừng. Broker giữ thông điệp trong DLQ
     * để người vận hành kiểm tra/gửi lại; chỉ ghi log rồi ACK sẽ làm mất thông điệp lỗi.
     */
    // Failed messages must remain available for inspection/replay, rather than being acknowledged after logging.
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_EMAIL_DLQ, autoStartup = "false")
    public void handleDeadLetterMessage(OrderPaidEvent event) {
        log.error("[DLQ ALERT] Phát hiện thông điệp lỗi trong Dead Letter Queue '{}'! Mã đơn hàng: '{}', Email khách hàng: '{}', Số tiền: '{}'",
                RabbitMQConfig.QUEUE_ORDER_EMAIL_DLQ,
                event.getOrderCode(),
                event.getCustomerEmail(),
                event.getTotalAmount());
    }
}
