package com.helishop.core.modules.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
public class OrderPaidOutboxDelivery {
    private final OrderPaidOutboxRepository outbox;
    private final ObjectMapper json;
    private final RabbitTemplate rabbit;

    @Transactional
    public void deliver(Long id) {
        var entry = outbox.findByIdWithLock(id).orElse(null);
        if (entry == null || (entry.isEmailPublished() && entry.isSellerPublished())) return;
        entry.setAttempts(entry.getAttempts() + 1);
        try {
            var event = json.readValue(entry.getPayload(), OrderPaidEvent.class);
            if (!entry.isEmailPublished()) {
                send(entry, event, RabbitMQConfig.ROUTING_ORDER_PAID_EMAIL);
                entry.setEmailPublished(true);
            }
            if (!entry.isSellerPublished()) {
                send(entry, event, RabbitMQConfig.ROUTING_ORDER_PAID_SELLER);
                entry.setSellerPublished(true);
            }
            entry.setLastError(null);
        } catch (RuntimeException | JsonProcessingException exception) {
            entry.setLastError("Chưa xác nhận được sự kiện, sẽ thử lại");
            log.warn("Outbox id={} chưa gửi xong, lần thử={}", entry.getId(), entry.getAttempts());
        }
        outbox.save(entry);
    }

    private void send(OrderPaidOutbox entry, OrderPaidEvent event, String routingKey) {
        rabbit.invoke(operations -> {
            operations.convertAndSend(RabbitMQConfig.EXCHANGE_ORDER_DIRECT, routingKey, event, message -> {
                message.getMessageProperties().setMessageId("order-paid-" + entry.getId());
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            });
            operations.waitForConfirmsOrDie(5000);
            return null;
        });
    }
}
