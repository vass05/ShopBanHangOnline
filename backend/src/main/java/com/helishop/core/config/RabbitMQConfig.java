package com.helishop.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String EXCHANGE_ORDER_DIRECT = "order.direct.exchange";
    public static final String EXCHANGE_ORDER_DLX = "order.dlx.exchange";

    // Routing Keys
    public static final String ROUTING_ORDER_PAID_EMAIL = "order.paid.email";
    public static final String ROUTING_ORDER_EMAIL_DLQ = "order.email.dlq";
    public static final String ROUTING_ORDER_PAID_SELLER = "order.paid.seller";

    // Queue names
    public static final String QUEUE_ORDER_EMAIL = "order.email.queue";
    public static final String QUEUE_ORDER_EMAIL_DLQ = "order.email.dlq";
    public static final String QUEUE_ORDER_SELLER_NOTIFY = "order.seller-notify.queue";

    // 1. Exchanges
    @Bean
    public DirectExchange orderDirectExchange() {
        return new DirectExchange(EXCHANGE_ORDER_DIRECT, true, false);
    }

    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(EXCHANGE_ORDER_DLX, true, false);
    }

    // 2. Queues
    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_EMAIL)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_ORDER_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue orderEmailDlq() {
        return QueueBuilder.durable(QUEUE_ORDER_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue orderSellerNotifyQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_SELLER_NOTIFY)
                .build();
    }

    // 3. Bindings
    @Bean
    public Binding orderEmailBinding(Queue orderEmailQueue, DirectExchange orderDirectExchange) {
        return BindingBuilder.bind(orderEmailQueue)
                .to(orderDirectExchange)
                .with(ROUTING_ORDER_PAID_EMAIL);
    }

    @Bean
    public Binding orderEmailDlqBinding(Queue orderEmailDlq, DirectExchange orderDlxExchange) {
        return BindingBuilder.bind(orderEmailDlq)
                .to(orderDlxExchange)
                .with(ROUTING_ORDER_EMAIL_DLQ);
    }

    @Bean
    public Binding orderSellerNotifyBinding(Queue orderSellerNotifyQueue, DirectExchange orderDirectExchange) {
        return BindingBuilder.bind(orderSellerNotifyQueue)
                .to(orderDirectExchange)
                .with(ROUTING_ORDER_PAID_SELLER);
    }

    // 4. Message Converter (JSON Serialization)
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
