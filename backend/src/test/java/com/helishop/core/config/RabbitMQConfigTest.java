package com.helishop.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Cấu hình DirectExchange và DLX DirectExchange chính xác tên và tính bền vững")
    void testExchanges() {
        DirectExchange directExchange = rabbitMQConfig.orderDirectExchange();
        assertThat(directExchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_ORDER_DIRECT);
        assertThat(directExchange.isDurable()).isTrue();

        DirectExchange dlxExchange = rabbitMQConfig.orderDlxExchange();
        assertThat(dlxExchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_ORDER_DLX);
        assertThat(dlxExchange.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Cấu hình Queue order.email.queue kèm Dead Letter Exchange và Routing Key chính xác")
    void testOrderEmailQueueWithDlx() {
        Queue emailQueue = rabbitMQConfig.orderEmailQueue();
        assertThat(emailQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_ORDER_EMAIL);
        assertThat(emailQueue.isDurable()).isTrue();
        assertThat(emailQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMQConfig.EXCHANGE_ORDER_DLX)
                .containsEntry("x-dead-letter-routing-key", RabbitMQConfig.ROUTING_ORDER_EMAIL_DLQ);

        Queue dlq = rabbitMQConfig.orderEmailDlq();
        assertThat(dlq.getName()).isEqualTo(RabbitMQConfig.QUEUE_ORDER_EMAIL_DLQ);
        assertThat(dlq.isDurable()).isTrue();

        Queue sellerNotifyQueue = rabbitMQConfig.orderSellerNotifyQueue();
        assertThat(sellerNotifyQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_ORDER_SELLER_NOTIFY);
        assertThat(sellerNotifyQueue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Cấu hình Bindings liên kết Queue vào Exchange với Routing Key chuẩn")
    void testBindings() {
        DirectExchange directExchange = rabbitMQConfig.orderDirectExchange();
        DirectExchange dlxExchange = rabbitMQConfig.orderDlxExchange();

        Queue emailQueue = rabbitMQConfig.orderEmailQueue();
        Queue dlq = rabbitMQConfig.orderEmailDlq();
        Queue sellerNotifyQueue = rabbitMQConfig.orderSellerNotifyQueue();

        Binding emailBinding = rabbitMQConfig.orderEmailBinding(emailQueue, directExchange);
        assertThat(emailBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_ORDER_EMAIL);
        assertThat(emailBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE_ORDER_DIRECT);
        assertThat(emailBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.ROUTING_ORDER_PAID_EMAIL);

        Binding dlqBinding = rabbitMQConfig.orderEmailDlqBinding(dlq, dlxExchange);
        assertThat(dlqBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_ORDER_EMAIL_DLQ);
        assertThat(dlqBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE_ORDER_DLX);
        assertThat(dlqBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.ROUTING_ORDER_EMAIL_DLQ);

        Binding sellerNotifyBinding = rabbitMQConfig.orderSellerNotifyBinding(sellerNotifyQueue, directExchange);
        assertThat(sellerNotifyBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_ORDER_SELLER_NOTIFY);
        assertThat(sellerNotifyBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE_ORDER_DIRECT);
        assertThat(sellerNotifyBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.ROUTING_ORDER_PAID_SELLER);
    }

    @Test
    @DisplayName("Cấu hình MessageConverter sử dụng Jackson2JsonMessageConverter")
    void testMessageConverter() {
        MessageConverter converter = rabbitMQConfig.jackson2JsonMessageConverter();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
