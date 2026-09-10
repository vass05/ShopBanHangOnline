package com.helishop.core.modules.payment.event;

import com.helishop.core.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Bắn sự kiện đơn hàng đã thanh toán thành công vào RabbitMQ Exchange
     */
    public void publishOrderPaidEvent(OrderPaidEvent event) {
        log.info("Phát sự kiện OrderPaidEvent cho đơn hàng '{}' (ID: {}) tới exchange '{}'",
                event.getOrderCode(), event.getOrderId(), RabbitMQConfig.EXCHANGE_ORDER_DIRECT);

        try {
            // 1. Gửi tới queue xử lý gửi Email xác nhận
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_ORDER_DIRECT,
                    RabbitMQConfig.ROUTING_ORDER_PAID_EMAIL,
                    event
            );

            // 2. Gửi tới queue thông báo cho Seller (nếu có shopId)
            if (event.getShopId() != null) {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_ORDER_DIRECT,
                        RabbitMQConfig.ROUTING_ORDER_PAID_SELLER,
                        event
                );
            }
        } catch (Exception e) {
            log.error("Lỗi khi bắn sự kiện OrderPaidEvent vào RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
